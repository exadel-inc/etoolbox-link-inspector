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

import com.day.crx.JcrConstants;
import com.exadel.etoolbox.linkinspector.api.Status;
import com.exadel.etoolbox.linkinspector.core.services.data.models.UpdatedItem;
import com.exadel.etoolbox.linkinspector.core.services.data.DataFeedService;
import com.exadel.etoolbox.linkinspector.core.services.helpers.LinkHelper;
import com.exadel.etoolbox.linkinspector.core.services.helpers.RepositoryHelper;
import com.exadel.etoolbox.linkinspector.core.services.util.ServletUtil;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.servlet.Servlet;
import java.io.IOException;
import java.util.Collections;

/**
 * Replaces a broken link with the new one within the specified resource property. The resource path,
 * property name, link for replacement and the desired link are passed from js during ajax call.
 * <p><u>Note</u>: This class is not a part of the public API and is subject to change. Do not use it in your own code</p>
 */
@Component(service = {Servlet.class})
@SlingServletResourceTypes(
        resourceTypes = "/bin/etoolbox/link-inspector/fix-broken-link",
        methods = HttpConstants.METHOD_POST
)
public class FixBrokenLinkServlet extends SlingAllMethodsServlet {
    private static final Logger LOG = LoggerFactory.getLogger(FixBrokenLinkServlet.class);

    private static final String PATH_PARAM = "path";
    private static final String PROPERTY_NAME_PARAM = "propertyName";
    private static final String CURRENT_LINK_PARAM = "currentLink";
    private static final String NEW_LINK_PARAM = "newLink";
    private static final String IS_SKIP_VALIDATION_PARAM = "isSkipValidation";
    private static final String STATUS_CODE_RESP_PARAM = "statusCode";
    private static final String STATUS_MSG_RESP_PARAM = "statusMessage";

    @Reference
    private transient LinkHelper linkHelper;

    @Reference
    private transient DataFeedService dataFeedService;

    @Reference
    private transient RepositoryHelper repositoryHelper;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        try {
            String path = ServletUtil.getRequestParamString(request, PATH_PARAM);
            String propertyName = ServletUtil.getRequestParamString(request, PROPERTY_NAME_PARAM);
            String currentLink = ServletUtil.getRequestParamString(request, CURRENT_LINK_PARAM);
            String newLink = ServletUtil.getRequestParamString(request, NEW_LINK_PARAM);
            boolean isSkipValidation = ServletUtil.getRequestParamBoolean(request, IS_SKIP_VALIDATION_PARAM);

            if (StringUtils.isAnyBlank(path, propertyName, currentLink, newLink)) {
                response.setStatus(HttpStatus.SC_UNPROCESSABLE_ENTITY);
                LOG.warn("Any (or all) request params are empty: path - {}, propertyName - {}, currentLink - {}, newLink - {}",
                        path, propertyName, currentLink, newLink);
                return;
            }
            if (currentLink.equals(newLink)) {
                response.setStatus(HttpStatus.SC_ACCEPTED);
                LOG.debug("currentLink and newLink values are equal, no processing is required");
                return;
            }
            if (!isSkipValidation) {
                Status linkStatus = validateLink(newLink);
                if (!linkStatus.isValid()) {
                    response.setStatus(HttpStatus.SC_BAD_REQUEST);
                    linkStatusToResponse(linkStatus, response);
                    LOG.debug("The newLink is not valid");
                    return;
                }
            }

            ResourceResolver resourceResolver = request.getResourceResolver();
            if (linkHelper.replaceLink(resourceResolver, path, propertyName, currentLink, newLink)) {
                repositoryHelper.createResourceIfNotExist(DataFeedService.PENDING_GENERATION_NODE,
                        JcrConstants.NT_UNSTRUCTURED, JcrResourceConstants.NT_SLING_FOLDER);
                modifyDataFeed(path, propertyName, newLink);
                resourceResolver.commit();
                LOG.debug("The link was updated: path - {}, propertyName - {}, currentLink - {}, newLink - {}",
                        path, propertyName, currentLink, newLink);
            } else {
                LOG.debug("The current link {} was not updated", currentLink);
                response.setStatus(HttpStatus.SC_NO_CONTENT);
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private Status validateLink(String link) {
        try (ResourceResolver resourceResolver = repositoryHelper.getServiceResourceResolver()) {
            return linkHelper.validateLink(link, resourceResolver);
        }
    }

    private void linkStatusToResponse(Status linkStatus, SlingHttpServletResponse response) {
        String jsonResponse = Json.createObjectBuilder()
                .add(STATUS_CODE_RESP_PARAM, linkStatus.getCode())
                .add(STATUS_MSG_RESP_PARAM, linkStatus.getMessage())
                .build()
                .toString();
        ServletUtil.writeJsonResponse(response, jsonResponse);
    }

    private void modifyDataFeed(String path, String propertyName, String newLink) {
        UpdatedItem updatedItem = new UpdatedItem(StringUtils.EMPTY, newLink, path, propertyName);
        dataFeedService.modifyDataFeed(Collections.singletonList(updatedItem), false);
    }
}