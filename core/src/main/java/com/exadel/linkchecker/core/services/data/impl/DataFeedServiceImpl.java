package com.exadel.linkchecker.core.services.data.impl;

import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.exadel.linkchecker.core.models.GridViewItem;
import com.exadel.linkchecker.core.services.RepositoryHelper;
import com.exadel.linkchecker.core.services.data.models.GridResource;
import com.exadel.linkchecker.core.services.data.DataFeedService;
import com.exadel.linkchecker.core.services.data.GridResourcesGenerator;
import com.exadel.linkchecker.core.services.util.CsvUtil;
import com.exadel.linkchecker.core.services.util.JsonUtil;
import com.exadel.linkchecker.core.services.util.LinkCheckerResourceUtil;
import com.exadel.linkchecker.core.services.util.constants.GridResourceProperties;
import com.exadel.linkchecker.core.services.util.constants.CommonConstants;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.jcr.contentloader.ContentTypeUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component(service = DataFeedService.class)
public class DataFeedServiceImpl implements DataFeedService {
    private static final Logger LOG = LoggerFactory.getLogger(DataFeedService.class);

    @Reference
    private RepositoryHelper repositoryHelper;

    @Reference
    private GridResourcesGenerator gridResourcesGenerator;

    private static final String GRID_RESOURCE_TYPE = "exadel-linkchecker/components/gridConfig";
    public static final int UI_ITEMS_LIMIT = 500;

    public static final String JSON_FEED_PATH = "/apps/exadel-linkchecker/components/content/data/datafeed.json";

    public static final String CSV_REPORT_PATH = "/content/exadel-linkchecker/download/report.csv";
    private static final String[] CSV_COLUMNS = {
            "Link",
            "Code",
            "Status Message",
            "Page",
            "Page Path",
            "Component Name",
            "Component Type",
            "Property Location"
    };

    @Override
    public void generateDataFeed() {
        LOG.info("Start link checker data feed generation");
        try (ResourceResolver resourceResolver = repositoryHelper.getServiceResourceResolver()) {
            if (resourceResolver == null) {
                LOG.warn("ResourceResolver is null, data feed generation is stopped");
                return;
            }
            Optional.of(gridResourcesGenerator.generateGridResources(GRID_RESOURCE_TYPE, resourceResolver))
                    .filter(CollectionUtils::isNotEmpty)
                    .ifPresent(gridResources -> {
                        gridResourcesToDataFeed(gridResources, resourceResolver);
                        generateCsvReport(gridResources, resourceResolver);
                    });
            LOG.info("Link checker data feed generation is completed");
        }
    }

    @Override
    public List<Resource> dataFeedToResources() {
        LOG.debug("Start data feed to resources conversion");
        try (ResourceResolver serviceResourceResolver = repositoryHelper.getServiceResourceResolver()) {
            if (serviceResourceResolver == null) {
                LOG.warn("ResourceResolver is null, data feed to resources conversion is stopped");
                return Collections.emptyList();
            }
            List<Resource> resources = toSlingResourcesStream(dataFeedToGridResources(serviceResourceResolver, true),
                    repositoryHelper.getThreadResourceResolver())
                    .collect(Collectors.toList());
            LOG.info("Exadel Link Checker - the number of items shown on UI is {}", resources.size());
            return resources;
        }
    }

    @Override
    public List<GridResource> dataFeedToGridResources() {
        try (ResourceResolver serviceResourceResolver = repositoryHelper.getServiceResourceResolver()) {
            if (serviceResourceResolver == null) {
                LOG.warn("ResourceResolver is null, data feed to grid resources conversion is stopped");
                return Collections.emptyList();
            }
            return dataFeedToGridResources(serviceResourceResolver, false);
        }
    }

    private List<GridResource> dataFeedToGridResources(ResourceResolver resourceResolver, boolean limited) {
        Set<GridResource> gridResources = new HashSet<>();
        JSONArray jsonArray = JsonUtil.getJsonArrayFromFile(JSON_FEED_PATH, resourceResolver);
        int allItemsSize = jsonArray.length();
        if (allItemsSize > 0) {
            int limit = limited ? Math.min(allItemsSize, UI_ITEMS_LIMIT) : allItemsSize;
            for (int i = 0; i < limit; i++) {
                try {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    Optional.ofNullable(JsonUtil.jsonToModel(jsonObject, GridResource.class))
                            .ifPresent(gridResources::add);
                } catch (JSONException e) {
                    LOG.error("Failed to convert json object to GridResource", e);
                }
            }
        }
        return gridResources.stream()
                .sorted(Comparator.comparing(GridResource::getHref))
                .collect(Collectors.toList());
    }

    private void gridResourcesToDataFeed(Collection<GridResource> gridResources, ResourceResolver resourceResolver) {
        try {
            JSONArray resourcesJsonArray = JsonUtil.objectsToJsonArray(gridResources);
                removePreviousDataFeed(resourceResolver);
                saveGridResourcesToJcr(resourceResolver, resourcesJsonArray);
                removePendingNode(resourceResolver);
                resourceResolver.commit();
                LOG.debug("Saving data feed json in jcr completed, path {}", JSON_FEED_PATH);
        } catch (PersistenceException e) {
            LOG.error("Saving data feed json in jcr failed", e);
        }
    }

    private void removePreviousDataFeed(ResourceResolver resourceResolver) {
        LinkCheckerResourceUtil.removeResource(JSON_FEED_PATH, resourceResolver);
    }

    private void saveGridResourcesToJcr(ResourceResolver resourceResolver, JSONArray jsonArray) {
        LinkCheckerResourceUtil.saveFileToJCR(JSON_FEED_PATH, jsonArray.toString().getBytes(),
                ContentTypeUtil.TYPE_JSON, resourceResolver);
    }

    private void removePendingNode(ResourceResolver resourceResolver) {
        LinkCheckerResourceUtil.removeResource(CommonConstants.PENDING_GENERATION_NODE, resourceResolver);
    }

    private Stream<Resource> toSlingResourcesStream(Collection<GridResource> gridResources, ResourceResolver resourceResolver) {
        return gridResources.stream()
                .map(gridResource -> toSlingResource(gridResource, resourceResolver));
    }

    private Resource toSlingResource(GridResource gridResource, ResourceResolver resourceResolver) {
        ValueMap valueMap = new ValueMapDecorator(new HashMap<>());
        valueMap.put(GridResourceProperties.PN_LINK, gridResource.getHref());
        valueMap.put(GridResourceProperties.PN_LINK_TYPE, gridResource.getType());
        valueMap.put(GridResourceProperties.PN_LINK_STATUS_CODE, gridResource.getStatusCode());
        valueMap.put(GridResourceProperties.PN_LINK_STATUS_MESSAGE, gridResource.getStatusMessage());
        valueMap.put(GridResourceProperties.PN_RESOURCE_PATH, gridResource.getResourcePath());
        valueMap.put(GridResourceProperties.PN_PROPERTY_NAME, gridResource.getPropertyName());
        return new ValueMapResource(resourceResolver, gridResource.getResourcePath(), gridResource.getResourceType(), valueMap);
    }

    private void generateCsvReport(Collection<GridResource> gridResources, ResourceResolver resourceResolver) {
        StopWatch stopWatch = StopWatch.createStarted();
        LOG.debug("Generating CSV report");
        byte[] csvContentBytes = gridResourcesToCsv(gridResources, resourceResolver);
        if (csvContentBytes != null) {
            LinkCheckerResourceUtil.removeResource(CSV_REPORT_PATH, resourceResolver);
            LinkCheckerResourceUtil.saveFileToJCR(CSV_REPORT_PATH, csvContentBytes,
                    CsvUtil.CSV_MIME_TYPE, resourceResolver);
        }
        stopWatch.stop();
        LOG.debug("Generating CSV report is completed in {} ms", stopWatch.getTime(TimeUnit.MILLISECONDS));
    }

    private byte[] gridResourcesToCsv(Collection<GridResource> gridResources, ResourceResolver resourceResolver) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             CSVPrinter csvPrinter = new CSVPrinter(new PrintWriter(out), CSVFormat.DEFAULT.withHeader(CSV_COLUMNS))
        ) {
            toSlingResourcesStream(gridResources, resourceResolver)
                    .map(resource -> resource.adaptTo(GridViewItem.class))
                    .filter(Objects::nonNull)
                    .forEach(viewItem -> printGridResource(csvPrinter, viewItem));
            csvPrinter.flush();
            return out.toByteArray();
        } catch (IOException e) {
            LOG.error("Failed to build CSV for grid resources", e);
        }
        return null;
    }

    private void printGridResource(CSVPrinter csvPrinter, GridViewItem viewItem) {
        try {
            csvPrinter.printRecord(
                    CsvUtil.wrapIfContainsSemicolon(viewItem.getLink()),
                    viewItem.getLinkStatusCode(),
                    viewItem.getLinkStatusMessage(),
                    viewItem.getPageTitle(),
                    viewItem.getPagePath(),
                    viewItem.getComponentName(),
                    viewItem.getComponentType(),
                    CsvUtil.buildLocation(viewItem.getPath(), viewItem.getPropertyName())
            );
        } catch (IOException e) {
            LOG.error(String.format("Failed to build CSV for the grid resource %s", viewItem.getLink()), e);
        }
    }
}