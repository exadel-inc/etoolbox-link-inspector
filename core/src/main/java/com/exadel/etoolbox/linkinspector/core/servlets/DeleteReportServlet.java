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
import com.exadel.etoolbox.linkinspector.core.services.exceptions.DataFeedException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;

/**
 * Handles deletion of Link Inspector reports from the repository.
 * <p>
 * This servlet exposes an HTTP DELETE endpoint that removes all link inspection data
 * from the repository. It delegates the actual deletion operation to the {@link DataFeedService}
 * and handles any exceptions that might occur during the process.
 * <p>
 * The servlet is registered at the path "/bin/etoolbox/link-inspector/delete-report"
 * and is used by the Link Inspector UI to clear existing reports when requested by the user.
 * <p><u>Note</u>: This class is not a part of the public API and is subject to change. Do not use it in your own code</p>
 */
@Component(service = {Servlet.class},
        property = {
                "sling.servlet.methods=" + HttpConstants.METHOD_DELETE,
                "sling.servlet.paths=" + "/bin/etoolbox/link-inspector/delete-report"
        }
)
public class DeleteReportServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(DeleteReportServlet.class);

    @Reference
    private DataFeedService dataFeedService;

    @Override
    protected void doDelete(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        try {
            dataFeedService.deleteDataFeed();
            response.setStatus(HttpStatus.SC_OK);
        } catch (DataFeedException e) {
            LOG.error(e.getMessage());
            response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
