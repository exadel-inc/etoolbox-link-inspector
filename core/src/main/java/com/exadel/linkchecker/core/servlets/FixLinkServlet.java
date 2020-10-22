package com.exadel.linkchecker.core.servlets;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
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
import java.util.Optional;

@Component(service = {Servlet.class}, property = "sling.servlet.paths=" + "/bin/exadel/fix-broken-link")
@SlingServletResourceTypes(
        resourceTypes = "/bin/exadel/fix-broken-link",
        methods = HttpConstants.METHOD_POST
)
public class FixLinkServlet extends SlingAllMethodsServlet {
    private static final Logger LOG = LoggerFactory.getLogger(FixLinkServlet.class);

    private static final String PATH_PARAM = "path";
    private static final String CURRENT_ELEMENT_NAME_PARAM = "currentElementName";
    private static final String NEW_LINK_PARAM = "newLink";

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        try {
            ResourceResolver resourceResolver = request.getResourceResolver();
            String path = getRequestParamString(request, PATH_PARAM);
            if (StringUtils.isNotBlank(path)) {
                Resource resource = resourceResolver.getResource(path);
                if (resource != null) {
                    ModifiableValueMap map = resource.adaptTo(ModifiableValueMap.class);
                    if (map != null) {
                        map.put(getRequestParamString(request, CURRENT_ELEMENT_NAME_PARAM), getRequestParamString(request, NEW_LINK_PARAM));
                    }
                }
            }
            resourceResolver.commit();
        } catch (PersistenceException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private String getRequestParamString(SlingHttpServletRequest request, String param) {
        return Optional.ofNullable(request.getRequestParameter(param))
                .map(RequestParameter::getString)
                .orElse(StringUtils.EMPTY);
    }

}
