package com.exadel.linkchecker.core.servlets;

import com.exadel.linkchecker.core.services.helpers.RepositoryHelper;
import com.exadel.linkchecker.core.services.util.constants.CommonConstants;
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
        resourceTypes = "/bin/exadel/datafeed/pending-generation-check",
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
                    Optional.ofNullable(resourceResolver.getResource(CommonConstants.PENDING_GENERATION_NODE))
                            .isPresent();
            LOG.trace("Is pending node present: {} ", isPendingNodePresent);
            response.setStatus(isPendingNodePresent ? HttpStatus.SC_OK : HttpStatus.SC_NO_CONTENT);
        }
    }
}