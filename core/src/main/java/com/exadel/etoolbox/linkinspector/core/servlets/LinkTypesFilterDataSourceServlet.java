package com.exadel.etoolbox.linkinspector.core.servlets;

import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.day.cq.commons.jcr.JcrConstants;
import com.exadel.etoolbox.linkinspector.api.LinkResolver;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import javax.servlet.Servlet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component(service = {Servlet.class})
@SlingServletResourceTypes(
        resourceTypes = "/bin/etoolbox/link-inspector/settings/linktypes",
        methods = HttpConstants.METHOD_GET)
public class LinkTypesFilterDataSourceServlet extends SlingSafeMethodsServlet {
    @Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    private transient volatile List<LinkResolver> linkResolvers;

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response) {

        List<Resource> links = new ArrayList<>();
        for (LinkResolver linkResolver : linkResolvers) {
            if (!linkResolver.isEnabled()) {
                continue;
            }
            ValueMap valueMap = new ValueMapDecorator(new HashMap<>());
            valueMap.put("value", linkResolver.getId());
            valueMap.put("text", linkResolver.getId());
            links.add(new ValueMapResource(request.getResourceResolver(), new ResourceMetadata(), JcrConstants.NT_UNSTRUCTURED, valueMap));
        }

        DataSource dataSource = new SimpleDataSource(links.iterator());
        request.setAttribute(DataSource.class.getName(), dataSource);
    }
}
