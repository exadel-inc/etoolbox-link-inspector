/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exadel.etoolbox.linkinspector.core.servlets;

import com.exadel.etoolbox.linkinspector.core.services.data.DataFeedService;
import com.exadel.etoolbox.linkinspector.core.services.data.models.UpdatedItem;
import com.exadel.etoolbox.linkinspector.core.services.util.ServletUtil;
import lombok.extern.slf4j.Slf4j;
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

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Collections;

/**
 * Handles link value editing requests from the Link Inspector UI.
 * <p>
 * This servlet exposes an HTTP POST endpoint that allows updating link values in the repository.
 * It accepts parameters for the current link value, updated link value, resource path, and
 * property name. The servlet performs two main operations:
 * <ol>
 *   <li>Updates the link in the data feed through the {@link DataFeedService}</li>
 *   <li>Updates the actual property in the repository content</li>
 * </ol>
 * <p>
 * The servlet is registered at the path "/bin/etoolbox/link-inspector/edit-value" and is used
 * by the Link Inspector UI to implement the link editing functionality.
 * <p><u>Note</u>: This class is not a part of the public API and is subject to change. Do not use it in your own code</p>
 */
@Component(service = {Servlet.class})
@SlingServletResourceTypes(
        resourceTypes = "/bin/etoolbox/link-inspector/edit-value",
        methods = HttpConstants.METHOD_POST
)
@Slf4j
public class EditValueServlet extends SlingAllMethodsServlet {

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
            log.warn("A required parameter is missing");
            return;
        }
        UpdatedItem updatedItem = new UpdatedItem(currentLink, updatedLink, path, propertyName);
        dataFeedService.modifyDataFeed(Collections.singletonList(updatedItem), true);
        ResourceResolver resourceResolver = request.getResourceResolver();
        Resource resource = resourceResolver.getResource(path);
        if (resource == null) {
            response.setStatus(HttpStatus.SC_NOT_FOUND);
            log.warn("Resource not found by path: {}", path);
            return;
        }
        ModifiableValueMap modifiableValueMap = resource.adaptTo(ModifiableValueMap.class);
        if (modifiableValueMap == null) {
            response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            log.error("Resource is not adaptable to ModifiableValueMap: {}", resource.getPath());
            return;
        }
        modifiableValueMap.put(propertyName, updatedLink);
        resourceResolver.commit();
    }
}
