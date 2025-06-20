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
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;

import javax.servlet.Servlet;
import java.util.Collections;

/**
 * Servlet that handles requests to generate a link inspection data feed.
 * <p>
 * This servlet exposes an HTTP POST endpoint that initiates an asynchronous job
 * for generating a link inspection data feed. It uses the Sling Job framework to
 * create a background job that will be processed by the {@link DataFeedJobExecutor}.
 * <p>
 * Triggering data feed generation through this servlet allows the UI to request
 * a new link inspection run without blocking the user interface while the potentially
 * long-running operation completes.
 * <p>
 * The servlet is registered at the path "/bin/etoolbox/link-inspector/datafeed/generate"
 * and is used by the Link Inspector UI to manually trigger data generation.
 */
@Component(service = {Servlet.class})
@SlingServletResourceTypes(
        resourceTypes = "/bin/etoolbox/link-inspector/datafeed/generate",
        methods = HttpConstants.METHOD_POST
)
@ServiceDescription("The servlet for manual triggering data feed generation")
public class GenerateDataFeedServlet extends SlingAllMethodsServlet {

    @Reference
    private transient JobManager jobManager;

    @Override
    protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response) {
        SlingJobUtil.addJob(
                jobManager,
                DataFeedJobExecutor.GENERATE_DATA_FEED_TOPIC,
                Collections.emptyMap()
        );
    }
}