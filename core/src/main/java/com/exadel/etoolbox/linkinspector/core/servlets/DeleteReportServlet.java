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
