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
import java.util.Arrays;
import java.util.Optional;

@Component(service = {Servlet.class})
@SlingServletResourceTypes(
        resourceTypes = "/bin/exadel/fix-broken-link",
        methods = HttpConstants.METHOD_POST
)
public class FixLinkServlet extends SlingAllMethodsServlet {
    private static final Logger LOG = LoggerFactory.getLogger(FixLinkServlet.class);

    private static final String PATH_PARAM = "path";
    private static final String CURRENT_ELEMENT_NAME_PARAM = "currentElementName";
    private static final String CURRENT_LINK_PARAM = "currentLink";
    private static final String NEW_LINK_PARAM = "newLink";

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        try {
            ResourceResolver resourceResolver = request.getResourceResolver();
            String paths = getRequestParamString(request, PATH_PARAM);
            if (StringUtils.isNotBlank(paths)) {
                Resource resource = resourceResolver.getResource(paths);
                if (resource != null) {
                    ModifiableValueMap map = resource.adaptTo(ModifiableValueMap.class);
                    if (map != null) {
                        String elementName =getRequestParamString(request, CURRENT_ELEMENT_NAME_PARAM);
                        if (map.get(elementName) instanceof String[]) {
                            String[] multiple = (String[]) map.get(elementName);
                            for (int i = 0; i < multiple.length; i++) {
                                multiple[i] = multiple[i].replaceAll(getRequestParamString(request, CURRENT_LINK_PARAM), getRequestParamString(request, NEW_LINK_PARAM));
                            }
                            map.put(elementName, multiple);
                        }
                        else{
                            map.put(elementName, map.get(elementName).toString().replaceAll(getRequestParamString(request, CURRENT_LINK_PARAM), getRequestParamString(request, NEW_LINK_PARAM)));
                        }
                    }
                }
                resourceResolver.commit();
            }
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
