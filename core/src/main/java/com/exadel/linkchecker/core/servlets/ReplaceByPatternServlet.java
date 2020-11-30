package com.exadel.linkchecker.core.servlets;

import com.day.crx.JcrConstants;
import com.exadel.linkchecker.core.services.helpers.LinkHelper;
import com.exadel.linkchecker.core.services.helpers.RepositoryHelper;
import com.exadel.linkchecker.core.services.data.DataFeedService;
import com.exadel.linkchecker.core.services.data.models.GridResource;
import com.exadel.linkchecker.core.services.helpers.PackageHelper;
import com.exadel.linkchecker.core.services.util.CsvUtil;
import com.exadel.linkchecker.core.services.util.ServletUtil;
import com.exadel.linkchecker.core.services.util.constants.CommonConstants;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The servlet for replacement the detected broken links by pattern.
 * The link pattern and replacement are retrieved from UI dialog and passed from js during ajax call.
 */
@Component(service = {Servlet.class})
@SlingServletResourceTypes(
        resourceTypes = "/bin/exadel/replace-links-by-pattern",
        methods = HttpConstants.METHOD_POST
)
public class ReplaceByPatternServlet extends SlingAllMethodsServlet {
    private static final Logger LOG = LoggerFactory.getLogger(ReplaceByPatternServlet.class);

    public static final Integer COMMIT_THRESHOLD = 500;

    private static final String LINK_PATTERN_PARAM = "pattern";
    private static final String REPLACEMENT_PARAM = "replacement";
    private static final String BACKUP_PARAM = "isBackup";
    private static final String OUTPUT_AS_CSV_PARAM = "isOutputAsCsv";

    private static final String BACKUP_PACKAGE_GROUP = "Exadel Link Checker";
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

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        String linkPattern = ServletUtil.getRequestParamString(request, LINK_PATTERN_PARAM);
        String replacement = ServletUtil.getRequestParamString(request, REPLACEMENT_PARAM);
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

        try {
            ResourceResolver resourceResolver = request.getResourceResolver();
            List<GridResource> gridResources = dataFeedService.dataFeedToGridResources();
            List<UpdatedItem> updatedItems = processResources(resourceResolver, gridResources, linkPattern, replacement, isBackup);
            if (!updatedItems.isEmpty()) {
                repositoryHelper.createResourceIfNotExist(CommonConstants.PENDING_GENERATION_NODE,
                        JcrConstants.NT_UNSTRUCTURED, JcrResourceConstants.NT_SLING_FOLDER);
                if (resourceResolver.hasChanges()) {
                    resourceResolver.commit();
                }
                if (outputAsCsv) {
                    generateCsvOutput(updatedItems, response);
                }
            } else {
                LOG.debug("No links were updated, linkPattern: {}, replacement: {}", linkPattern, replacement);
                response.setStatus(HttpStatus.SC_NO_CONTENT);
            }
        } catch (PersistenceException e) {
            LOG.error(String.format("Replacement failed, pattern: %s, replacement: %s", linkPattern, replacement), e);
        } catch (IOException | RepositoryException | PackageException e) {
            LOG.error("Failed to create backup package, replacement by pattern was not applied");
        }
    }

    private List<UpdatedItem> processResources(ResourceResolver resourceResolver, Collection<GridResource> gridResources,
                                               String linkPattern, String replacement, boolean backup)
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
        return replaceLinksByPattern(resourceResolver, filteredGridResources, linkPattern, replacement);
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
                .collect(Collectors.toList());
    }

    private List<UpdatedItem> replaceLinksByPattern(ResourceResolver resourceResolver,
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
                if (updatedItems.size() % COMMIT_THRESHOLD == 0) {
                    resourceResolver.commit();
                }
            }
        }
        return updatedItems;
    }

    private void generateCsvOutput(List<UpdatedItem> updatedItems, SlingHttpServletResponse response) {
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
        packageHelper.createPackageForPaths(backupPaths, session,
                BACKUP_PACKAGE_GROUP,
                String.format(BACKUP_PACKAGE_NAME, System.currentTimeMillis()),
                BACKUP_PACKAGE_VERSION,
                true,
                true);
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