package com.exadel.linkchecker.core.servlets;

import com.day.crx.JcrConstants;
import com.exadel.linkchecker.core.services.LinkHelper;
import com.exadel.linkchecker.core.services.data.DataFeedService;
import com.exadel.linkchecker.core.services.data.models.GridResource;
import com.exadel.linkchecker.core.services.util.ServletUtil;
import com.exadel.linkchecker.core.services.util.constants.CommonConstants;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * The servlet for replacement broken link by pattern within the specified resource property.
 * The link pattern and replacement are retrieved from UI dialog and passed from js during ajax call.
 */
@Component(service = {Servlet.class})
@SlingServletResourceTypes(
        resourceTypes = "/bin/exadel/replace-links-by-pattern",
        methods = HttpConstants.METHOD_POST
)
public class UpdateAllLinksServlet extends SlingAllMethodsServlet {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateAllLinksServlet.class);

    private static final String LINK_PATTERN_PARAM = "pattern";
    private static final String REPLACEMENT_PARAM = "replacement";

    public static final Integer COMMIT_THRESHOLD = 500;

    @Reference
    private DataFeedService dataFeedService;

    @Reference
    private LinkHelper linkHelper;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        String linkPattern = ServletUtil.getRequestParamString(request, LINK_PATTERN_PARAM);
        String replacement = ServletUtil.getRequestParamString(request, REPLACEMENT_PARAM);
        if (StringUtils.isAnyBlank(linkPattern, replacement)) {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            LOG.warn("Any (or all) request params are empty: linkPattern - {}, replacement - {}",
                    linkPattern, replacement);
            return;
        }
        if (linkPattern.equals(replacement)) {
            response.setStatus(HttpStatus.SC_ACCEPTED);
            LOG.debug("linkPattern and replacement are equal, no processing is required");
            return;
        }
        try {
            ResourceResolver resourceResolver = request.getResourceResolver();
            List<GridResource> gridResources = dataFeedService.dataFeedToGridResources();
            int updatedLinksCount = replaceLinksByPattern(resourceResolver, gridResources, linkPattern, replacement);
            if (updatedLinksCount > 0) {
                ResourceUtil.getOrCreateResource(
                        resourceResolver,
                        CommonConstants.PENDING_GENERATION_NODE,
                        JcrConstants.NT_UNSTRUCTURED,
                        JcrResourceConstants.NT_SLING_FOLDER,
                        false
                );
                resourceResolver.commit();
            } else {
                LOG.debug("{} link(s) were updated, linkPattern: {}, replacement: {}",
                        updatedLinksCount, linkPattern, replacement);
                response.setStatus(HttpStatus.SC_NO_CONTENT);
            }
        } catch (PersistenceException e) {
            LOG.error(String.format("Replacement failed, pattern: %s, replacement: %s", linkPattern, replacement), e);
        }
    }

    private int replaceLinksByPattern(ResourceResolver resourceResolver, Collection<GridResource> gridResources,
                                      String linkPattern, String replacement) throws PersistenceException {
        int updatedLinksCounter = 0;
        for (GridResource gridResource : gridResources) {
            String currentLink = gridResource.getHref();
            String path = gridResource.getResourcePath();
            String propertyName = gridResource.getPropertyName();
            if (StringUtils.isAnyBlank(currentLink, path, propertyName)) {
                continue;
            }
            Optional<String> updated = Optional.of(currentLink.replaceAll(linkPattern, replacement))
                    .filter(updatedLink -> !updatedLink.equals(currentLink))
                    .filter(updatedLink ->
                            linkHelper.replaceLink(resourceResolver, path, propertyName, currentLink, updatedLink)
                    );
            if (updated.isPresent()) {
                updatedLinksCounter++;
                LOG.trace("The link was updated: location - {}@{}, currentLink - {}, updatedLink - {}",
                        path, propertyName, currentLink, updated.get());
                if (updatedLinksCounter % COMMIT_THRESHOLD == 0) {
                    resourceResolver.commit();
                }
            }
        }
        return updatedLinksCounter;
    }
}