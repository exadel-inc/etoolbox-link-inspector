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

package com.exadel.aembox.linkchecker.core.servlets;

import com.exadel.aembox.linkchecker.core.services.data.DataFeedService;
import com.exadel.aembox.linkchecker.core.services.helpers.RepositoryHelper;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.util.Optional;

/**
 * After fixing broken links the node, indicating that data feed regeneration is required in order to reflect
 * the latest changes, is created. The purpose of the servlet is to check presence of the mentioned node.
 */
@Component(service = {Servlet.class})
@SlingServletResourceTypes(
        resourceTypes = "/bin/aembox/linkchecker/pending-generation-check",
        methods = HttpConstants.METHOD_POST
)
@ServiceDescription("The servlet for checking if the pending generation node exists")
public class PendingGenerationCheckServlet extends SlingAllMethodsServlet {
    private static final Logger LOG = LoggerFactory.getLogger(PendingGenerationCheckServlet.class);

    @Reference
    private RepositoryHelper repositoryHelper;

    @Override
    protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response) {
        try (ResourceResolver resourceResolver = repositoryHelper.getServiceResourceResolver()) {
            boolean isPendingNodePresent =
                    Optional.ofNullable(resourceResolver.getResource(DataFeedService.PENDING_GENERATION_NODE))
                            .isPresent();
            LOG.trace("Is pending node present: {} ", isPendingNodePresent);
            response.setStatus(isPendingNodePresent ? HttpStatus.SC_OK : HttpStatus.SC_NO_CONTENT);
        }
    }
}