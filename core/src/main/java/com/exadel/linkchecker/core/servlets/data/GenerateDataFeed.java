package com.exadel.linkchecker.core.servlets.data;

import com.exadel.linkchecker.core.services.data.DataFeedService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;

import javax.servlet.Servlet;
@Component(service = {Servlet.class})
@SlingServletResourceTypes(
        resourceTypes = "/bin/exadel/generate-datafeed",
        methods = HttpConstants.METHOD_GET
)
@ServiceDescription("The servlet for manual triggering data feed generation")
public class GenerateDataFeed extends SlingSafeMethodsServlet {
    @Reference
    private DataFeedService dataFeedService;

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response) {
        dataFeedService.generateDataFeed();
    }
}