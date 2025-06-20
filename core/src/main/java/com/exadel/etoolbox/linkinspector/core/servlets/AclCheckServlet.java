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

import com.exadel.etoolbox.linkinspector.core.services.helpers.RepositoryHelper;
import com.exadel.etoolbox.linkinspector.core.services.util.ServletUtil;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import javax.json.Json;
import javax.servlet.Servlet;
import java.util.Optional;

/**
 * Servlet that checks if the current user has specific permissions at a given repository path.
 * <p>
 * This servlet exposes an HTTP POST endpoint that accepts a repository path and a comma-separated
 * list of permissions to check. It verifies whether the current user's session has the requested
 * permissions at the specified path and returns the result as a JSON response.
 * <p>
 * The servlet is registered at the path "/bin/etoolbox/link-inspector/acl-check" and is used
 * by the Link Inspector UI to verify access rights before attempting operations that require
 * specific permissions.
 */
@Component(service = {Servlet.class})
@SlingServletResourceTypes(
        resourceTypes = "/bin/etoolbox/link-inspector/acl-check",
        methods = HttpConstants.METHOD_POST
)
@ServiceDescription("The servlet for checking ACLs")
public class AclCheckServlet extends SlingAllMethodsServlet {
    private static final Logger LOG = LoggerFactory.getLogger(AclCheckServlet.class);

    private static final String PATH_PARAM = "path";
    private static final String PERMISSIONS_PARAM = "permissions";
    private static final String HAS_PERMISSIONS_RESPONSE_PARAM = "hasPermissions";

    @Reference
    private transient RepositoryHelper repositoryHelper;

    @Override
    protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response) {
        String path = ServletUtil.getRequestParamString(request, PATH_PARAM);
        String permissions = ServletUtil.getRequestParamString(request, PERMISSIONS_PARAM);
        if (StringUtils.isAnyBlank(path, permissions)) {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            LOG.warn("Path is blank, ACL check failed");
            return;
        }
        Optional<Session> session = Optional.ofNullable(request.getResourceResolver().adaptTo(Session.class));
        if (!session.isPresent()) {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            LOG.warn("ACL check failed, session is null. Path: {}", path);
            return;
        }

        boolean hasPermissions = repositoryHelper.hasPermissions(session.get(), path, permissions);
        String jsonResponse = Json.createObjectBuilder()
                .add(HAS_PERMISSIONS_RESPONSE_PARAM, hasPermissions)
                .build()
                .toString();
        ServletUtil.writeJsonResponse(response, jsonResponse);
    }
}