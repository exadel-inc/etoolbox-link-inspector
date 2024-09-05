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

package com.exadel.etoolbox.linkinspector.core.services.data.impl;

import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.exadel.etoolbox.linkinspector.core.models.ui.GridViewItem;
import com.exadel.etoolbox.linkinspector.core.services.data.DataFeedService;
import com.exadel.etoolbox.linkinspector.core.services.data.GridResourcesGenerator;
import com.exadel.etoolbox.linkinspector.core.services.data.models.DataFilter;
import com.exadel.etoolbox.linkinspector.core.services.exceptions.DataFeedException;
import com.exadel.etoolbox.linkinspector.core.services.helpers.LinkHelper;
import com.exadel.etoolbox.linkinspector.core.services.util.CsvUtil;
import com.exadel.etoolbox.linkinspector.core.services.helpers.RepositoryHelper;
import com.exadel.etoolbox.linkinspector.core.services.data.models.GridResource;
import com.exadel.etoolbox.linkinspector.core.services.util.JsonUtil;
import com.exadel.etoolbox.linkinspector.core.services.util.LinkInspectorResourceUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
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
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implements {@link DataFeedService} interface to provide an OSGi service which is responsible for managing the data
 * feed based on the set of resources generated by ${@link GridResourcesGenerator}
 */
@Component(service = DataFeedService.class)
public class DataFeedServiceImpl implements DataFeedService {
    private static final Logger LOG = LoggerFactory.getLogger(DataFeedServiceImpl.class);
    private static final String BROKEN_LINKS_MAP_KEY = "elc-broken-links";

    @SuppressWarnings("UnstableApiUsage")
    private Cache<String, CopyOnWriteArrayList<GridResource>> gridResourcesCache;

    @Reference
    private RepositoryHelper repositoryHelper;

    @Reference
    private GridResourcesGenerator gridResourcesGenerator;

    @Reference
    private LinkHelper linkHelper;

    /**
     * The sling resource type of grid row items
     */
    private static final String GRID_RESOURCE_TYPE = "etoolbox-link-inspector/components/gridConfig";

    /**
     * The location of the data feed json in the repository
     */
    private static final String JSON_FEED_PATH = "/content/etoolbox-link-inspector/data/datafeed.json";

    /**
     * The location of the generated Csv report in the repository
     */
    private static final String CSV_REPORT_PATH = "/content/etoolbox-link-inspector/download/report.csv";

    /**
     * The columns represented in the Csv report
     */
    private static final String[] CSV_COLUMNS = {
            "Link",
            "Type",
            "Code",
            "Status Message",
            "Page",
            "Page Path",
            "Component Name",
            "Component Type",
            "Property Location"
    };

    @Activate
    @SuppressWarnings("unused")
    private void activate() {
        gridResourcesCache = CacheBuilder.newBuilder()
                .maximumSize(1)
                .expireAfterWrite(100000, TimeUnit.DAYS)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void generateDataFeed() {
        LOG.info("Start link inspector data feed generation");
        try (ResourceResolver resourceResolver = repositoryHelper.getServiceResourceResolver()) {
            if (resourceResolver == null) {
                LOG.warn("ResourceResolver is null, data feed generation is stopped");
                return;
            }
            Optional.ofNullable(gridResourcesGenerator.generateGridResources(GRID_RESOURCE_TYPE, resourceResolver))
                    .ifPresent(gridResources -> {
                        setGridResourcesList(gridResources);
                        gridResourcesToDataFeed(gridResources, resourceResolver);
                        generateCsvReport(gridResources, resourceResolver);
                    });
            LOG.info("Link inspector data feed generation is completed");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Resource> dataFeedToResources(DataFilter filter) {
        LOG.debug("Start data feed to resources conversion");
        try (ResourceResolver serviceResourceResolver = repositoryHelper.getServiceResourceResolver()) {
            if (serviceResourceResolver == null) {
                LOG.warn("ResourceResolver is null, data feed to resources conversion is stopped");
                return Collections.emptyList();
            }
            if (CollectionUtils.isEmpty(getGridResourcesList())) {
                setGridResourcesList(dataFeedToGridResources(serviceResourceResolver));
            }
            List<Resource> resources = toSlingResourcesStream(
                    doFiltering(getGridResourcesList(), filter),
                    repositoryHelper.getThreadResourceResolver())
                    .collect(Collectors.toList());
            LOG.info("EToolbox Link Inspector - the number of items shown is {}", resources.size());
            return resources;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<GridResource> dataFeedToGridResources() {
        try (ResourceResolver serviceResourceResolver = repositoryHelper.getServiceResourceResolver()) {
            if (serviceResourceResolver == null) {
                LOG.warn("ResourceResolver is null, data feed to grid resources conversion is stopped");
                return Collections.emptyList();
            }
            return dataFeedToGridResources(serviceResourceResolver);
        }
    }

    @Override
    public void modifyDataFeed(Map<String, String> valuesMap) {
        List<GridResource> gridResources = getGridResourcesList();
        try (ResourceResolver serviceResourceResolver = repositoryHelper.getServiceResourceResolver()) {
            for (GridResource gridResource : gridResources) {
                String propertyAddress = gridResource.getResourcePath() +"@" + gridResource.getPropertyName();
                String propertyValue = valuesMap.getOrDefault(propertyAddress, StringUtils.EMPTY);
                if (propertyValue.isEmpty()) {
                    continue;
                }
                linkHelper
                        .getLinkStream(propertyValue)
                        .forEach(link -> {
                            linkHelper.validateLink(link, serviceResourceResolver);
                            link.setStatus("Modified");
                            gridResource.setLink(link);
                        });
            }
            setGridResourcesList(gridResources);
            gridResourcesToDataFeed(gridResources, serviceResourceResolver);
        }
    }

    @Override
    public void deleteDataFeed() throws DataFeedException {
        try (ResourceResolver serviceResourceResolver = repositoryHelper.getServiceResourceResolver()) {
            removePreviousDataFeed(serviceResourceResolver);
            removeCsvReport(serviceResourceResolver);
            removePendingNode(serviceResourceResolver);
            clearCache();
            serviceResourceResolver.commit();
        } catch (PersistenceException e) {
            LOG.error("Failed to delete data feed", e);
            throw new DataFeedException("Exception Deleting Data Feed");
        }
    }

    private CopyOnWriteArrayList<GridResource> getGridResourcesList() {
        return gridResourcesCache.asMap().getOrDefault(BROKEN_LINKS_MAP_KEY, new CopyOnWriteArrayList<>());
    }

    private synchronized void setGridResourcesList(List<GridResource> gridResources) {
        gridResourcesCache.asMap().put(BROKEN_LINKS_MAP_KEY, new CopyOnWriteArrayList<>(gridResources));
    }

    private void clearCache() {
        gridResourcesCache.invalidateAll();
    }

    private List<GridResource> dataFeedToGridResources(ResourceResolver resourceResolver) {
        List<GridResource> gridResources = new ArrayList<>();
        JSONArray jsonArray = JsonUtil.getJsonArrayFromFile(JSON_FEED_PATH, resourceResolver);
        int allItemsSize = jsonArray.length();
        if (allItemsSize > 0) {
            for (int i = 0; i < allItemsSize; i++) {
                try {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    Optional.ofNullable(JsonUtil.jsonToModel(jsonObject, GridResource.class))
                            .ifPresent(gridResources::add);
                } catch (JSONException e) {
                    LOG.error("Failed to convert json object to GridResource", e);
                }
            }
        }
        return gridResources;
    }

    private synchronized void gridResourcesToDataFeed(Collection<GridResource> gridResources, ResourceResolver resourceResolver) {
        try {
            JSONArray resourcesJsonArray = JsonUtil.objectsToJsonArray(gridResources);
            removePreviousDataFeed(resourceResolver);
            saveGridResourcesToJcr(resourceResolver, resourcesJsonArray);
            removePendingNode(resourceResolver);
            resourceResolver.commit();
            LOG.debug("Saving data feed json to jcr completed, path {}", JSON_FEED_PATH);
        } catch (PersistenceException e) {
            LOG.error("Saving data feed json to jcr failed", e);
        }
    }

    private void removePreviousDataFeed(ResourceResolver resourceResolver) {
        LinkInspectorResourceUtil.removeResource(JSON_FEED_PATH, resourceResolver);
    }

    private void removeCsvReport(ResourceResolver resourceResolver) {
        LinkInspectorResourceUtil.removeResource(CSV_REPORT_PATH, resourceResolver);
    }

    private void saveGridResourcesToJcr(ResourceResolver resourceResolver, JSONArray jsonArray) {
        LinkInspectorResourceUtil.saveFileToJCR(
                JSON_FEED_PATH,
                jsonArray.toString().getBytes(StandardCharsets.UTF_8),
                ContentTypeUtil.TYPE_JSON,
                resourceResolver
        );
    }

    private void removePendingNode(ResourceResolver resourceResolver) {
        LinkInspectorResourceUtil.removeResource(DataFeedService.PENDING_GENERATION_NODE, resourceResolver);
    }

    private Stream<Resource> toSlingResourcesStream(Collection<GridResource> gridResources, ResourceResolver resourceResolver) {
        return gridResources.stream()
                .map(gridResource -> toSlingResource(gridResource, resourceResolver));
    }

    private Resource toSlingResource(GridResource gridResource, ResourceResolver resourceResolver) {
        ValueMap valueMap = new ValueMapDecorator(new HashMap<>());
        valueMap.put(GridResource.PN_LINK, gridResource.getHref());
        valueMap.put(GridResource.PN_LINK_TYPE, gridResource.getType());
        valueMap.put(GridResource.PN_LINK_STATUS_CODE, gridResource.getStatusCode());
        valueMap.put(GridResource.PN_LINK_STATUS_MESSAGE, gridResource.getStatusMessage());
        valueMap.put(GridResource.PN_RESOURCE_PATH, gridResource.getResourcePath());
        valueMap.put(GridResource.PN_PROPERTY_NAME, gridResource.getPropertyName());
        return new ValueMapResource(resourceResolver, gridResource.getResourcePath(), gridResource.getResourceType(), valueMap);
    }

    private void generateCsvReport(Collection<GridResource> gridResources, ResourceResolver resourceResolver) {
        StopWatch stopWatch = StopWatch.createStarted();
        LOG.debug("Start CSV report generation");
        List<GridViewItem> gridViewItems = toSlingResourcesStream(gridResources, resourceResolver)
                .map(resource -> resource.adaptTo(GridViewItem.class))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        byte[] csvContentBytes = CsvUtil.itemsToCsvByteArray(gridViewItems, this::printViewItemToCsv, CSV_COLUMNS);
        LinkInspectorResourceUtil.removeResource(CSV_REPORT_PATH, resourceResolver);
        LinkInspectorResourceUtil.saveFileToJCR(CSV_REPORT_PATH, csvContentBytes,
                CsvUtil.CSV_MIME_TYPE, resourceResolver);
        stopWatch.stop();
        LOG.debug("Generation of CSV report is completed in {} ms", stopWatch.getTime(TimeUnit.MILLISECONDS));
    }

    private void printViewItemToCsv(CSVPrinter csvPrinter, GridViewItem viewItem) {
        try {
            csvPrinter.printRecord(
                    CsvUtil.wrapIfContainsSemicolon(viewItem.getLink()),
                    viewItem.getLinkType(),
                    viewItem.getLinkStatusCode(),
                    CsvUtil.wrapIfContainsSemicolon(viewItem.getLinkStatusMessage()),
                    CsvUtil.wrapIfContainsSemicolon(viewItem.getPageTitle()),
                    viewItem.getPagePath(),
                    CsvUtil.wrapIfContainsSemicolon(viewItem.getComponentName()),
                    viewItem.getComponentType(),
                    CsvUtil.buildLocation(viewItem.getPath(), viewItem.getPropertyName())
            );
        } catch (IOException e) {
            LOG.error(String.format("Failed to build CSV for the grid resource %s", viewItem.getLink()), e);
        }
    }

    private List<GridResource> doFiltering(List<GridResource> resources, DataFilter filter) {
        return resources.stream().filter(filter::validate).collect(Collectors.toList());
    }
}