package com.exadel.etoolbox.linkinspector.core.servlets;

import com.exadel.etoolbox.linkinspector.core.services.data.DataFeedService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;

@Component(service = {Servlet.class},
        property = {
                "sling.servlet.methods=" + HttpConstants.METHOD_DELETE,
                "sling.servlet.paths=" + "/bin/etoolbox/link-inspector/delete-report"
        }
)
public class DeleteReportServlet extends SlingAllMethodsServlet {

    @Reference
    private DataFeedService dataFeedService;

    @Override
    protected void doDelete(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        dataFeedService.deleteDataFeed();
    }
}
