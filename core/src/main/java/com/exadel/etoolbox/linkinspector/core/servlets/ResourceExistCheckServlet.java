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

import com.exadel.etoolbox.linkinspector.core.services.util.ServletUtil;
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

/**
 * Servlet that verifies the existence of a repository resource at a specified path.
 * <p>
 * This servlet exposes an HTTP POST endpoint that accepts a repository path parameter
 * and checks whether a resource exists at that location. The result is returned as a
 * JSON object with a boolean flag indicating whether the resource was found.
 * <p>
 * The servlet is registered at the path "/bin/etoolbox/link-inspector/resource-exist-check"
 * and is used by the Link Inspector UI to validate resource paths before performing
 * operations that depend on resource existence.
 */
@Component(service = {Servlet.class})
@SlingServletResourceTypes(
        resourceTypes = "/bin/etoolbox/link-inspector/resource-exist-check",
        methods = HttpConstants.METHOD_POST
)
@ServiceDescription("The servlet for checking if resource exists")
public class ResourceExistCheckServlet extends SlingAllMethodsServlet {
    private static final Logger LOG = LoggerFactory.getLogger(ResourceExistCheckServlet.class);

    private static final String PATH_PARAM = "path";
    private static final String RESOURCE_EXISTS_RESP_PARAM = "resourceExists";

    @Override
    protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response) {
        String path = ServletUtil.getRequestParamString(request, PATH_PARAM);
        if (StringUtils.isBlank(path)) {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            LOG.warn("Path is blank, resource existence check failed");
            return;
        }
        boolean resourceExists = Optional.of(request.getResourceResolver())
                .map(resourceResolver -> resourceResolver.getResource(path))
                .isPresent();
        String jsonResponse = Json.createObjectBuilder()
                .add(RESOURCE_EXISTS_RESP_PARAM, resourceExists)
                .build()
                .toString();
        ServletUtil.writeJsonResponse(response, jsonResponse);
    }
}