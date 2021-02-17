package com.exadel.aembox.linkchecker.core.servlets;

import com.exadel.aembox.linkchecker.core.services.util.ServletUtil;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.servlet.Servlet;
import java.util.Optional;

@Component(service = {Servlet.class})
@SlingServletResourceTypes(
        resourceTypes = "/bin/aembox/linkchecker/resource-exist-check",
        methods = HttpConstants.METHOD_POST
)
@ServiceDescription("The servlet for checking if resource exists")
public class ResourceExistCheckServlet extends SlingAllMethodsServlet {
    private static final Logger LOG = LoggerFactory.getLogger(ResourceExistCheckServlet.class);

    private static final String PATH_PARAM = "path";
    private static final String RESOURCE_EXISTS_RESP_PARAM = "isResourceExist";

    @Override
    protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response) {
        String path = ServletUtil.getRequestParamString(request, PATH_PARAM);
        if (StringUtils.isBlank(path)) {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            LOG.warn("Path is blank, resource existence check failed");
            return;
        }
        boolean isResourceExist = Optional.of(request.getResourceResolver())
                .map(resourceResolver -> resourceResolver.getResource(path))
                .isPresent();
        String jsonResponse = Json.createObjectBuilder()
                .add(RESOURCE_EXISTS_RESP_PARAM, isResourceExist)
                .build()
                .toString();
        ServletUtil.writeJsonResponse(response, jsonResponse);
    }
}