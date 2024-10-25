package com.exadel.etoolbox.linkinspector.core.servlets;

import com.exadel.etoolbox.linkinspector.api.LinkResolver;
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
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

@Component(service = {Servlet.class})
@SlingServletResourceTypes(
        resourceTypes = "/bin/etoolbox/link-inspector/config",
        methods = HttpConstants.METHOD_POST
)
public class ServicesReloaderServlet extends SlingAllMethodsServlet {

    @Reference
    private LinkResolver linkResolver;

    private BundleContext bundleContext;

    @Activate
    private void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        ServiceComponentRuntime scr = getServiceComponentRuntime();
        ComponentDescriptionDTO dto = getComponentDescription(scr, linkResolver.getClass());
        if (dto != null) {
            CountDownLatch latch = new CountDownLatch(1);
            scr.disableComponent(dto)
                    .then((promise) -> {
                        return scr.enableComponent(dto);
                    })
                    .then((promise) -> {
                        response.getWriter().write("Component restarted!");
                        return null;
                    })
                    .onResolve(latch::countDown);
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new IOException(e);
            }
        } else {
            response.getWriter().write("Component not found!");
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
