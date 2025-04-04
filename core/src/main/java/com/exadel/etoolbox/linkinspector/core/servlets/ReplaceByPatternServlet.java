/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exadel.etoolbox.linkinspector.core.servlets;

import com.day.crx.JcrConstants;
import com.exadel.etoolbox.linkinspector.core.services.data.models.UpdatedItem;
import com.exadel.etoolbox.linkinspector.core.services.data.DataFeedService;
import com.exadel.etoolbox.linkinspector.core.services.data.models.GridResource;
import com.exadel.etoolbox.linkinspector.core.services.helpers.LinkHelper;
import com.exadel.etoolbox.linkinspector.core.services.helpers.PackageHelper;
import com.exadel.etoolbox.linkinspector.core.services.helpers.RepositoryHelper;
import com.exadel.etoolbox.linkinspector.core.services.util.CsvUtil;
import com.exadel.etoolbox.linkinspector.core.services.util.ServletUtil;
import org.apache.commons.collections4.CollectionUtils;
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
import org.osgi.service.component.annotations.Deactivate;
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
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Performs replacement by pattern within the detected broken links scope.
 * The link pattern and replacement are retrieved from the UI dialog and passed to the servlet via an ajax call.
 */
@Component(service = {Servlet.class})
@SlingServletResourceTypes(
        resourceTypes = "/bin/etoolbox/link-inspector/replace-links-by-pattern",
        methods = HttpConstants.METHOD_POST
)
@Designate(ocd = ReplaceByPatternServlet.Configuration.class)
public class ReplaceByPatternServlet extends SlingAllMethodsServlet {

    @ObjectClassDefinition(
            name = "EToolbox Link Inspector - Replace By Pattern Servlet",
            description = "The servlet for replacement the detected broken links by pattern"
    )
    @interface Configuration {

        @AttributeDefinition(
                name = "Commit Threshold",
                description = "The size of updated items chunks saved via resourceResolver.commit()"
        ) int commitThreshold() default DEFAULT_COMMIT_THRESHOLD;
    }

    private static final Logger LOG = LoggerFactory.getLogger(ReplaceByPatternServlet.class);

    private static final int DEFAULT_COMMIT_THRESHOLD = 1000;
    private static final String LINK_PATTERN_PARAM = "pattern";
    private static final String REPLACEMENT_PARAM = "replacement";
    private static final String DRY_RUN_PARAM = "isDryRun";
    private static final String BACKUP_PARAM = "isBackup";
    private static final String OUTPUT_AS_CSV_PARAM = "isOutputAsCsv";
    private static final String ADVANCED_MODE_PARAM = "advancedMode";
    private static final String ITEMS_COUNT_RESP_PARAM = "updatedItemsCount";
    private static final String BACKUP_PACKAGE_GROUP = "EToolbox_Link_Inspector";
    private static final String BACKUP_PACKAGE_NAME = "replace_by_pattern_backup_%s";
    private static final String BACKUP_PACKAGE_VERSION = "1.0";
    private static final String SELECTED_PARAM = "selected";
    private static final String LINKS_PARAM = "links";

    private static final String[] CSV_COLUMNS = {
            "Link",
            "Updated Link",
            "Location"
    };

    @Reference
    private transient DataFeedService dataFeedService;

    @Reference
    private transient LinkHelper linkHelper;

    @Reference
    private transient RepositoryHelper repositoryHelper;

    @Reference
    private transient PackageHelper packageHelper;

    private volatile boolean isDeactivated;

    private int commitThreshold;

    @Activate
    @Modified
    void activate(Configuration configuration) {
        commitThreshold = configuration.commitThreshold();
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        String linkPattern = ServletUtil.getRequestParamString(request, LINK_PATTERN_PARAM);
        String replacement = ServletUtil.getRequestParamString(request, REPLACEMENT_PARAM);
        boolean isAdvancedMode = ServletUtil.getRequestParamBoolean(request, ADVANCED_MODE_PARAM);
        boolean isDryRun = ServletUtil.getRequestParamBoolean(request, DRY_RUN_PARAM);
        boolean isBackup = ServletUtil.getRequestParamBoolean(request, BACKUP_PARAM);
        boolean isOutputAsCsv = ServletUtil.getRequestParamBoolean(request, OUTPUT_AS_CSV_PARAM);
        List<String> selectedItemsProperties = ServletUtil.getRequestParamStringList(request, SELECTED_PARAM);
        List<String> selectedItemsLinks = ServletUtil.getRequestParamStringList(request, LINKS_PARAM);

        if (StringUtils.isBlank(replacement)) {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            LOG.warn("Request params is empty: replacement - {}", replacement);
            return;
        }

        if (isAdvancedMode && StringUtils.isBlank(linkPattern)) {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            LOG.warn("Request params is empty: linkPattern - {}", linkPattern);
            return;
        }
        if (isAdvancedMode && linkPattern.equals(replacement)) {
            response.setStatus(HttpStatus.SC_ACCEPTED);
            LOG.debug("linkPattern and replacement are equal, no processing is required");
            return;
        }

        StopWatch stopWatch = StopWatch.createStarted();
        LOG.info("Starting replacement by pattern, linkPattern: {}, replacement: {}", linkPattern, replacement);
        try {
            ResourceResolver resourceResolver = request.getResourceResolver();
            List<GridResource> filteredGridResources = getFilteredGridResources(selectedItemsProperties, selectedItemsLinks);
            List<UpdatedItem> updatedItems =
                    processResources(filteredGridResources, isDryRun, isBackup, isAdvancedMode, linkPattern, replacement, resourceResolver);
            if (CollectionUtils.isEmpty(updatedItems)) {
                LOG.info("No links were updated, linkPattern: {}, replacement: {}", linkPattern, replacement);
                response.setStatus(HttpStatus.SC_NO_CONTENT);
                return;
            }
            if (!isDryRun) {
                dataFeedService.modifyDataFeed(updatedItems, false);
            }
            outputUpdatedItems(updatedItems, isDryRun, isOutputAsCsv, linkPattern, replacement, resourceResolver, response);
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

    private List<UpdatedItem> processResources(Collection<GridResource> gridResources,
                                           boolean isDryRun,
                                           boolean isBackup,
                                           boolean isAdvancedMode,
                                           String linkPattern,
                                           String replacement,
                                           ResourceResolver resourceResolver)
            throws IOException, RepositoryException, PackageException {
        Optional<Session> session = Optional.ofNullable(resourceResolver.adaptTo(Session.class));
        if (!session.isPresent()) {
            LOG.warn("Replacement failed, session is null. Pattern: {}, replacement: {}", linkPattern, replacement);
            return Collections.emptyList();
        }
        List<GridResource> filteredGridResources = filterGridResources(gridResources, linkPattern, session.get());
        if (isBackup && !isDeactivated) {
            createBackupPackage(filteredGridResources, session.get());
        }
        return replaceByPattern(filteredGridResources, isDryRun, isAdvancedMode, linkPattern, replacement, resourceResolver);
    }

    private List<GridResource> filterGridResources(Collection<GridResource> gridResources,
                                                   String linkPattern,
                                                   Session session) {
        Pattern pattern = Pattern.compile(linkPattern);
        return gridResources.stream()
                .filter(gridResource ->
                        StringUtils.isNoneBlank(gridResource.getValue(), gridResource.getResourcePath(), gridResource.getPropertyName())
                )
                .filter(gridResource -> pattern.matcher(gridResource.getValue()).find())
                .filter(gridResource ->
                        repositoryHelper.hasReadWritePermissions(session, gridResource.getResourcePath()))
                .collect(Collectors.toList());
    }

    private List<UpdatedItem> replaceByPattern(Collection<GridResource> gridResources,
                                           boolean isDryRun,
                                           boolean isAdvancedMode,
                                           String linkPattern,
                                           String replacement,
                                           ResourceResolver resourceResolver) throws PersistenceException {
        List<UpdatedItem> updatedItems = new ArrayList<>();
        for (GridResource gridResource : gridResources) {
            if (isDeactivated) {
                break;
            }
            String currentLink = gridResource.getValue();
            String path = gridResource.getResourcePath();
            String propertyName = gridResource.getPropertyName();
            Optional<String> updated = Optional.of(isAdvancedMode ? currentLink.replaceAll(linkPattern, replacement) : replacement)
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

    private void outputUpdatedItems(List<UpdatedItem> updatedItems,
                                    boolean isDryRun,
                                    boolean isOutputAsCsv,
                                    String linkPattern,
                                    String replacement,
                                    ResourceResolver resourceResolver,
                                    SlingHttpServletResponse response) throws PersistenceException {
        if (isDeactivated) {
            LOG.info("The service has been deactivated, replacement by pattern might have been completed partially");
            response.setStatus(HttpStatus.SC_NO_CONTENT);
            return;
        }
        if (!isDryRun) {
            repositoryHelper.createResourceIfNotExist(DataFeedService.PENDING_GENERATION_NODE,
                    JcrConstants.NT_UNSTRUCTURED, JcrResourceConstants.NT_SLING_FOLDER);
            if (resourceResolver.hasChanges()) {
                resourceResolver.commit();
            }
        }
        if (isOutputAsCsv) {
            generateCsvOutput(updatedItems, response);
        } else {
            itemsCountToResponse(updatedItems.size(), response);
        }
        LOG.info("{} link(s) were updated, linkPattern: {}, replacement: {}",
                updatedItems.size(),
                linkPattern,
                replacement);
    }

    private void generateCsvOutput(List<UpdatedItem> linkDtos, SlingHttpServletResponse response) {
        StopWatch stopWatch = StopWatch.createStarted();
        LOG.debug("Starting CSV output generation, the number of updated items: {}", linkDtos.size());
        try {
            byte[] csvOutput = CsvUtil.itemsToCsvByteArray(linkDtos, this::printUpdatedItemToCsv, CSV_COLUMNS);
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
            LOG.error("Failed to download output as CSV, the number of updated locations: {}", linkDtos.size());
        }
        stopWatch.stop();
        LOG.debug("CSV output generation is finished in {} ms", stopWatch.getTime(TimeUnit.MILLISECONDS));
    }

    private void printUpdatedItemToCsv(CSVPrinter csvPrinter, UpdatedItem item) {
        try {
            csvPrinter.printRecord(
                    CsvUtil.wrapIfContainsSemicolon(item.getCurrentLink()),
                    CsvUtil.wrapIfContainsSemicolon(item.getUpdatedLink()),
                    item.getPropertyLocation()
            );
        } catch (IOException e) {
            LOG.error(String.format("Failed to build CSV for the item %s", item.getCurrentLink()), e);
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

    private List<GridResource> getFilteredGridResources(List<String> selectedItemsProperties, List<String> selectedItemsLinks) {
        return dataFeedService.dataFeedToGridResources()
                .stream().filter(gridResource -> selectedItemsProperties.contains(String
                        .format("%s@%s", gridResource.getResourcePath(), gridResource.getPropertyName())))
                .filter(gridResource -> selectedItemsLinks.contains(gridResource.getValue()))
                .collect(Collectors.toList());
    }

    @Deactivate
    protected void deactivate() {
        isDeactivated = true;
        LOG.debug("ReplaceByPatternServlet - deactivated");
    }
}