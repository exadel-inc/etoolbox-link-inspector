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

import com.exadel.etoolbox.linkinspector.core.services.job.DataFeedJobExecutor;
import com.exadel.etoolbox.linkinspector.core.services.job.SlingJobUtil;
import com.exadel.etoolbox.linkinspector.core.services.util.ServletUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.json.Json;
import javax.servlet.Servlet;

/**
 * Servlet that retrieves and provides information about the status of data feed generation jobs.
 * <p>
 * This servlet exposes an HTTP GET endpoint that queries the Sling Job Manager for information
 * about jobs with the data feed generation topic. It returns the current status of any active or
 * recent jobs as a JSON response, allowing the UI to display progress information to users.
 * <p>
 * The servlet is registered at the path "/bin/etoolbox/link-inspector/job-status" and is used
 * by the Link Inspector UI to show job status indicators during data feed generation.
 */
@Component(service = {Servlet.class})
@SlingServletResourceTypes(
        resourceTypes = "/bin/etoolbox/link-inspector/job-status",
        methods = HttpConstants.METHOD_GET
)
public class JobStatusServlet extends SlingAllMethodsServlet {

    private static final String JOB_STATUS = "status";

    @Reference
    private JobManager jobManager;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {

        String jobStatus = SlingJobUtil.getJobStatus(jobManager, DataFeedJobExecutor.GENERATE_DATA_FEED_TOPIC);

        String jsonResponse = Json.createObjectBuilder()
                .add(JOB_STATUS, jobStatus)
                .build()
                .toString();

        ServletUtil.writeJsonResponse(response, jsonResponse);
    }
}
