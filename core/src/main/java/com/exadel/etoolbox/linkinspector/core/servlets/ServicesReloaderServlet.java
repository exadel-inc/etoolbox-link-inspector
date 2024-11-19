package com.exadel.etoolbox.linkinspector.core.servlets;

import com.exadel.etoolbox.linkinspector.api.LinkResolver;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@Component(service = {Servlet.class})
@SlingServletResourceTypes(
        resourceTypes = "/bin/etoolbox/link-inspector/config",
        methods = HttpConstants.METHOD_POST
)
@Slf4j
public class ServicesReloaderServlet extends SlingAllMethodsServlet {

    @Reference(
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY
    )
    private transient volatile List<LinkResolver> linkResolvers;

    private BundleContext bundleContext;

    @Activate
    private void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    protected void doPost(
            @NonNull SlingHttpServletRequest request,
            @NonNull SlingHttpServletResponse response) throws ServletException, IOException {
        ServiceComponentRuntime scr = getServiceComponentRuntime();
        CountDownLatch latch = new CountDownLatch(linkResolvers.size());
        for (LinkResolver linkResolver : linkResolvers) {
            ComponentDescriptionDTO dto = getComponentDescription(scr, linkResolver.getClass());
            if (dto == null) {
                log.warn("Component description not found for {}", linkResolver.getClass());
                latch.countDown();
                continue;
            }
            log.info("Restarting component {}", dto.name);
            scr.disableComponent(dto)
                    .then((promise) -> scr.enableComponent(dto))
                    .then((promise) -> {
                        log.info("Component {} restarted", dto.name);
                        return null;
                    })
                    .onResolve(latch::countDown);
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    private ServiceComponentRuntime getServiceComponentRuntime(){
        ServiceReference<?> reference = bundleContext.getServiceReference(ServiceComponentRuntime.class.getName());
        return (ServiceComponentRuntime) bundleContext.getService(reference);
    }

    private ComponentDescriptionDTO getComponentDescription(ServiceComponentRuntime scr, Class<?> serviceType) {
        for (Bundle bundle : bundleContext.getBundles()) {
            ComponentDescriptionDTO dto = scr.getComponentDescriptionDTO(bundle, serviceType.getName());
            if (dto != null) {
                return dto;
            }
        }
        return null;
    }
}
