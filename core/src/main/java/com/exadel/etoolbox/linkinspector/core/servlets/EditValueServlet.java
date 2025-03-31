package com.exadel.etoolbox.linkinspector.core.servlets;

import com.exadel.etoolbox.linkinspector.core.services.data.DataFeedService;
import com.exadel.etoolbox.linkinspector.core.services.data.models.UpdatedItem;
import com.exadel.etoolbox.linkinspector.core.services.util.ServletUtil;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Collections;

@Component(service = {Servlet.class})
@SlingServletResourceTypes(
        resourceTypes = "/bin/etoolbox/link-inspector/edit-value",
        methods = HttpConstants.METHOD_POST
)
public class EditValueServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(EditValueServlet.class);

    @Reference
    private transient DataFeedService dataFeedService;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        String currentLink = ServletUtil.getRequestParamString(request, "currentLink");
        String updatedLink = ServletUtil.getRequestParamString(request, "updatedLink");
        String path = ServletUtil.getRequestParamString(request, "path");
        String propertyName = StringUtils.substringAfterLast(ServletUtil.getRequestParamString(request, "propertyName"), "/");
        if (StringUtils.isAnyBlank(currentLink, updatedLink, path, propertyName)) {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            LOG.warn("Request params is empty");
            return;
        }
        UpdatedItem updatedItem = new UpdatedItem(currentLink, updatedLink, path, propertyName);
        dataFeedService.modifyDataFeed(Collections.singletonList(updatedItem), true);
        ResourceResolver resourceResolver = request.getResourceResolver();
        Resource resource = resourceResolver.getResource(path);
        if (resource == null) {
            response.setStatus(HttpStatus.SC_NOT_FOUND);
            LOG.warn("Resource not found by path: {}", path);
            return;
        }
        ModifiableValueMap modifiableValueMap = resource.adaptTo(ModifiableValueMap.class);
        if (modifiableValueMap == null) {
            response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            LOG.error("Resource is not adaptable to ModifiableValueMap: {}", resource.getPath());
            return;
        }
        modifiableValueMap.put(propertyName, updatedLink);
        resourceResolver.commit();
    }
}
