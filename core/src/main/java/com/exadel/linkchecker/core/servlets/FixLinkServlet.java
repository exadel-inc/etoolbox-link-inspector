package com.exadel.linkchecker.core.servlets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;

@Component(service = {Servlet.class}, property = "sling.servlet.paths=" + "/bin/exadel/fix-broken-link")
@SlingServletResourceTypes(
        resourceTypes = "/bin/exadel/fix-broken-link",
        methods = HttpConstants.METHOD_POST
)
public class FixLinkServlet extends SlingAllMethodsServlet {
    private static final Logger log = LoggerFactory.getLogger(FixLinkServlet.class);

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        try {
            ResourceResolver resourceResolver = request.getResourceResolver();
            Resource resource = resourceResolver.getResource(request.getRequestParameter("path").getString());
            ModifiableValueMap map = resource.adaptTo(ModifiableValueMap.class);
            map.put(request.getRequestParameter("currentElementName").getString(), request.getRequestParameter("newLink").getString());
            resourceResolver.commit();
        } catch (PersistenceException e) {
            log.error(e.getMessage(), e);
        }
    }
}
