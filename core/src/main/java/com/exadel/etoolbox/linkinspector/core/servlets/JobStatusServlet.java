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

@Component(service = {Servlet.class})
@SlingServletResourceTypes(
        resourceTypes = "/bin/etoolbox/link-inspector/job-status",
        methods = HttpConstants.METHOD_GET
)
public class JobStatusServlet extends SlingAllMethodsServlet {

    public static final String JOB_STATUS = "status";
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
