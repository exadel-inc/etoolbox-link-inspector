package com.exadel.etoolbox.linkinspector.core.servlets;

import com.exadel.etoolbox.linkinspector.core.services.data.DataFeedService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;

@Component(service = {Servlet.class})
@SlingServletResourceTypes(
        resourceTypes = "/bin/etoolbox/link-inspector/delete-report",
        methods = HttpConstants.METHOD_GET
)
public class DeleteReportServlet extends SlingAllMethodsServlet {

    @Reference
    private DataFeedService dataFeedService;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        dataFeedService.deleteDataFeed();
    }
}
