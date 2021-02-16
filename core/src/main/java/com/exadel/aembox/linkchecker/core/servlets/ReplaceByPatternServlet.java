package com.exadel.aembox.linkchecker.core.servlets;

import com.day.crx.JcrConstants;
import com.exadel.aembox.linkchecker.core.services.data.DataFeedService;
import com.exadel.aembox.linkchecker.core.services.data.models.GridResource;
import com.exadel.aembox.linkchecker.core.services.helpers.LinkHelper;
import com.exadel.aembox.linkchecker.core.services.helpers.PackageHelper;
import com.exadel.aembox.linkchecker.core.services.helpers.RepositoryHelper;
import com.exadel.aembox.linkchecker.core.services.util.CsvUtil;
import com.exadel.aembox.linkchecker.core.services.util.ServletUtil;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.json.Json;
import javax.servlet.Servlet;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The servlet for replacement the detected broken links by pattern.
 * The link pattern and replacement are retrieved from UI dialog and passed from js during ajax call.
 */
@Component(service = {Servlet.class})
@SlingServletResourceTypes(
        resourceTypes = "/bin/aembox/replace-links-by-pattern",
        methods = HttpConstants.METHOD_POST
)
@Designate(ocd = ReplaceByPatternServlet.Configuration.class)
public class ReplaceByPatternServlet extends SlingAllMethodsServlet {
    @ObjectClassDefinition(
            name = "AEMBox Link Checker - Replace By Pattern Servlet",
            description = "The servlet for replacement the detected broken links by pattern"
    )
    @interface Configuration {
        @AttributeDefinition(
                name = "Limit",
                description = "The maximum number of items which replacement will be applied for"
        ) int max_updated_items_count() default DEFAULT_MAX_UPDATED_ITEMS_COUNT;

        @AttributeDefinition(
                name = "Commit Threshold",
                description = "The size of updated items chunks saved via resourceResolver.commit()"
        ) int commit_threshold() default DEFAULT_COMMIT_THRESHOLD;
    }

    private static final Logger LOG = LoggerFactory.getLogger(ReplaceByPatternServlet.class);

    public static final int DEFAULT_COMMIT_THRESHOLD = 1000;
    public static final int DEFAULT_MAX_UPDATED_ITEMS_COUNT = 10000;

    private static final String LINK_PATTERN_PARAM = "pattern";
    private static final String REPLACEMENT_PARAM = "replacement";
    private static final String DRY_RUN_PARAM = "isDryRun";
    private static final String BACKUP_PARAM = "isBackup";
    private static final String OUTPUT_AS_CSV_PARAM = "isOutputAsCsv";
    private static final String ITEMS_COUNT_RESP_PARAM = "updatedItemsCount";

    private static final String BACKUP_PACKAGE_GROUP = "Advanced Link Checker";
    private static final String BACKUP_PACKAGE_NAME = "replace_by_pattern_backup_%s";
    private static final String BACKUP_PACKAGE_VERSION = "1.0";

    private static final String[] CSV_COLUMNS = {
            "Link",
            "Updated Link",
            "Location"
    };

    @Reference
    private DataFeedService dataFeedService;

    @Reference
    private LinkHelper linkHelper;

    @Reference
    private RepositoryHelper repositoryHelper;

    @Reference
    private PackageHelper packageHelper;

    private int maxUpdatedItemsCount;
    private int commitThreshold;

    @Activate
    @Modified
    protected void activate(Configuration configuration) {
        maxUpdatedItemsCount = configuration.max_updated_items_count();
        commitThreshold = configuration.commit_threshold();
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        String linkPattern = ServletUtil.getRequestParamString(request, LINK_PATTERN_PARAM);
        String replacement = ServletUtil.getRequestParamString(request, REPLACEMENT_PARAM);
        boolean isDryRun = ServletUtil.getRequestParamBoolean(request, DRY_RUN_PARAM);
        boolean isBackup = ServletUtil.getRequestParamBoolean(request, BACKUP_PARAM);
        boolean outputAsCsv = ServletUtil.getRequestParamBoolean(request, OUTPUT_AS_CSV_PARAM);

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

        StopWatch stopWatch = StopWatch.createStarted();
        LOG.info("Starting replacement by pattern, linkPattern: {}, replacement: {}", linkPattern, replacement);
        try {
            ResourceResolver resourceResolver = request.getResourceResolver();
            List<GridResource> gridResources = dataFeedService.dataFeedToGridResources();
            List<UpdatedItem> updatedItems =
                    processResources(resourceResolver, gridResources, linkPattern, replacement, isDryRun, isBackup);
            if (!updatedItems.isEmpty()) {
                if (!isDryRun) {
                    repositoryHelper.createResourceIfNotExist(DataFeedService.PENDING_GENERATION_NODE,
                            JcrConstants.NT_UNSTRUCTURED, JcrResourceConstants.NT_SLING_FOLDER);
                    if (resourceResolver.hasChanges()) {
                        resourceResolver.commit();
                    }
                }
                if (outputAsCsv) {
                    generateCsvOutput(updatedItems, response);
                } else {
                    itemsCountToResponse(updatedItems.size(), response);
                }
                LOG.info("{} link(s) were updated, linkPattern: {}, replacement: {}", updatedItems.size(), linkPattern,
                        replacement);
            } else {
                LOG.info("No links were updated, linkPattern: {}, replacement: {}", linkPattern, replacement);
                response.setStatus(HttpStatus.SC_NO_CONTENT);
            }
            stopWatch.stop();
            LOG.info("Replacement by pattern is finished in {} ms", stopWatch.getTime(TimeUnit.MILLISECONDS));
        } catch (PersistenceException e) {
            LOG.error(String.format("Replacement failed, pattern: %s, replacement: %s", linkPattern, replacement), e);
            response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        } catch (IOException | RepositoryException | PackageException e) {
            LOG.error("Failed to create backup package, replacement by pattern was not applied", e);
            response.setStatus(HttpStatus.SC_FORBIDDEN);
        }
    }

    private List<UpdatedItem> processResources(ResourceResolver resourceResolver, Collection<GridResource> gridResources,
                                               String linkPattern, String replacement, boolean isDryRun, boolean backup)
            throws IOException, RepositoryException, PackageException {
        Optional<Session> session = Optional.ofNullable(resourceResolver.adaptTo(Session.class));
        if (!session.isPresent()) {
            LOG.warn("Replacement failed, session is null. Pattern: {}, replacement: {}", linkPattern, replacement);
            return Collections.emptyList();
        }
        List<GridResource> filteredGridResources = filterGridResources(gridResources, linkPattern, session.get());
        if (backup) {
            createBackupPackage(filteredGridResources, session.get());
        }
        return replaceByPattern(isDryRun, resourceResolver, filteredGridResources, linkPattern, replacement);
    }

    private List<GridResource> filterGridResources(Collection<GridResource> gridResources, String linkPattern, Session session) {
        Pattern pattern = Pattern.compile(linkPattern);
        return gridResources.stream()
                .filter(gridResource ->
                        StringUtils.isNoneBlank(gridResource.getHref(), gridResource.getResourcePath(), gridResource.getPropertyName())
                )
                .filter(gridResource -> pattern.matcher(gridResource.getHref()).find())
                .filter(gridResource ->
                        repositoryHelper.hasReadWritePermissions(session, gridResource.getResourcePath()))
                .limit(maxUpdatedItemsCount)
                .collect(Collectors.toList());
    }

    private List<UpdatedItem> replaceByPattern(boolean isDryRun,
                                               ResourceResolver resourceResolver,
                                               Collection<GridResource> gridResources,
                                               String linkPattern, String replacement) throws PersistenceException {
        List<UpdatedItem> updatedItems = new ArrayList<>();
        for (GridResource gridResource : gridResources) {
            String currentLink = gridResource.getHref();
            String path = gridResource.getResourcePath();
            String propertyName = gridResource.getPropertyName();
            Optional<String> updated = Optional.of(currentLink.replaceAll(linkPattern, replacement))
                    .filter(updatedLink -> !updatedLink.equals(currentLink))
                    .filter(updatedLink ->
                            linkHelper.replaceLink(resourceResolver, path, propertyName, currentLink, updatedLink)
                    );
            if (updated.isPresent()) {
                updatedItems.add(new UpdatedItem(currentLink, updated.get(), path, propertyName));
                LOG.trace("The link was updated: location - {}@{}, currentLink - {}, updatedLink - {}",
                        path, propertyName, currentLink, updated.get());
                if (!isDryRun && updatedItems.size() % commitThreshold == 0) {
                    resourceResolver.commit();
                }
            }
        }
        return updatedItems;
    }

    private void generateCsvOutput(List<UpdatedItem> updatedItems, SlingHttpServletResponse response) {
        StopWatch stopWatch = StopWatch.createStarted();
        LOG.debug("Starting CSV output generation, the number of updated items: {}", updatedItems.size());
        try {
            byte[] csvOutput = CsvUtil.itemsToCsvByteArray(updatedItems, this::printUpdatedItemToCsv, CSV_COLUMNS);
            if (ArrayUtils.isNotEmpty(csvOutput)) {
                response.setContentType(CsvUtil.CSV_MIME_TYPE);
                response.setContentLength(csvOutput.length);
                response.setHeader(FileUploadBase.CONTENT_DISPOSITION, FileUploadBase.ATTACHMENT);
                OutputStream outputStream = response.getOutputStream();
                outputStream.write(csvOutput);
                outputStream.flush();
            } else {
                LOG.debug("Failed to download output as CSV, output byte array is empty");
            }
        } catch (IOException e) {
            LOG.error("Failed to download output as CSV, the number of updated locations: {}", updatedItems.size());
        }
        stopWatch.stop();
        LOG.debug("CSV output generation is finished in {} ms", stopWatch.getTime(TimeUnit.MILLISECONDS));
    }

    private void printUpdatedItemToCsv(CSVPrinter csvPrinter, UpdatedItem item) {
        try {
            csvPrinter.printRecord(
                    CsvUtil.wrapIfContainsSemicolon(item.currentLink),
                    CsvUtil.wrapIfContainsSemicolon(item.updatedLink),
                    CsvUtil.buildLocation(item.path, item.propertyName)
            );
        } catch (IOException e) {
            LOG.error(String.format("Failed to build CSV for the item %s", item.currentLink), e);
        }
    }

    void createBackupPackage(List<GridResource> filteredGridResources, Session session) throws RepositoryException,
            PackageException, IOException {
        Set<String> backupPaths = filteredGridResources.stream()
                .map(GridResource::getResourcePath)
                .collect(Collectors.toSet());
        if (!backupPaths.isEmpty()) {
            StopWatch stopWatch = StopWatch.createStarted();
            LOG.debug("Starting backup package creation/build, the number of paths: {}", backupPaths.size());
            packageHelper.createPackageForPaths(backupPaths, session,
                    BACKUP_PACKAGE_GROUP,
                    String.format(BACKUP_PACKAGE_NAME, System.currentTimeMillis()),
                    BACKUP_PACKAGE_VERSION,
                    true,
                    true);
            stopWatch.stop();
            LOG.debug("Backup package build is finished in {} ms", stopWatch.getTime(TimeUnit.MILLISECONDS));
        }
    }

    private void itemsCountToResponse(int count, SlingHttpServletResponse response) {
        String jsonResponse = Json.createObjectBuilder()
                .add(ITEMS_COUNT_RESP_PARAM, count)
                .build()
                .toString();
        ServletUtil.writeJsonResponse(response, jsonResponse);
    }

    private static class UpdatedItem {
        private final String currentLink;
        private final String updatedLink;
        private final String path;
        private final String propertyName;

        public UpdatedItem(String currentLink, String updatedLink, String path, String propertyName) {
            this.currentLink = currentLink;
            this.updatedLink = updatedLink;
            this.path = path;
            this.propertyName = propertyName;
        }
    }
}