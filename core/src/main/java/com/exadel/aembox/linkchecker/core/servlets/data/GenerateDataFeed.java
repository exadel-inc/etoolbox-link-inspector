package com.exadel.aembox.linkchecker.core.servlets.data;

import com.exadel.aembox.linkchecker.core.services.job.DataFeedJobExecutor;
import com.exadel.aembox.linkchecker.core.services.job.SlingJobUtil;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.Servlet;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;

@Component(service = {Servlet.class})
@SlingServletResourceTypes(
        resourceTypes = "/bin/aembox/linkchecker/datafeed/generate",
        methods = HttpConstants.METHOD_GET
)
@ServiceDescription("The servlet for manual triggering data feed generation")
public class GenerateDataFeed extends SlingSafeMethodsServlet {
    private static final Logger LOG = LoggerFactory.getLogger(GenerateDataFeed.class);

    private static final String ADMIN_GROUP_ID = "administrators";

    @Reference
    private JobManager jobManager;

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response) {
        boolean isAdmin = Optional.of(request.getResourceResolver())
                .map(resourceResolver -> resourceResolver.adaptTo(User.class))
                .filter(this::isAdminUser)
                .isPresent();
        if (isAdmin) {
            SlingJobUtil.addJob(
                    jobManager,
                    DataFeedJobExecutor.GENERATE_DATA_FEED_TOPIC,
                    Collections.emptyMap()
            );
        } else {
            LOG.debug("Data feed generation wsa not triggered, user is not admin nor a member of '{}' group",
                    ADMIN_GROUP_ID);
        }
    }

    private boolean isAdminUser(User user) {
        if (user.isAdmin()) {
            return true;
        }
        try {
            Iterator<Group> groupIterator = user.memberOf();
            while (groupIterator.hasNext()) {
                String groupId = groupIterator.next().getID();
                if (groupId.equals(ADMIN_GROUP_ID)) {
                    return true;
                }
            }
        } catch (RepositoryException e) {
            LOG.error("Failed to check if user is admin", e);
        }
        return false;
    }
}