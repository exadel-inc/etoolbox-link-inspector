package com.exadel.linkchecker.core.services.data.impl;

import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.exadel.linkchecker.core.services.data.models.GridResource;
import com.exadel.linkchecker.core.services.data.DataFeedService;
import com.exadel.linkchecker.core.services.data.GridResourcesGenerator;
import com.exadel.linkchecker.core.services.util.JsonUtil;
import com.exadel.linkchecker.core.services.util.LinkCheckerResourceUtil;
import com.exadel.linkchecker.core.services.util.constants.GridResourceProperties;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections.CollectionUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
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

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component(service = DataFeedService.class)
public class DataFeedServiceImpl implements DataFeedService {
    private static final Logger LOG = LoggerFactory.getLogger(DataFeedService.class);

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private GridResourcesGenerator gridResourcesGenerator;

    private static final String LINK_CHECKER_SERVICE_NAME = "exadel-linkchecker-service";

    private static final String JSON_FILE_PATH = "/apps/linkchecker/components/content/data/datafeed.json";

    private static final String GRID_RESOURCE_TYPE = "linkchecker/components/gridConfig";

    private static final int UI_ITEMS_LIMIT = 500;

    @Override
    public void generateDataFeed() {
        LOG.info("Start link checker data feed generation");
        try (ResourceResolver resourceResolver = getResourceResolver()) {
            if (resourceResolver == null) {
                LOG.warn("ResourceResolver is null, data feed generation is stopped");
                return;
            }
            Optional.of(gridResourcesGenerator.generateGridResources(GRID_RESOURCE_TYPE, resourceResolver))
                    .filter(CollectionUtils::isNotEmpty)
                    .ifPresent(gridResources -> gridResourcesToDataFeed(gridResources, resourceResolver));
            LOG.info("Data feed was successfully generated and saved in {}", JSON_FILE_PATH);
        }
    }

    @Override
    public List<Resource> dataFeedToResources() {
        LOG.debug("Start data feed to resources conversion");
        try (ResourceResolver serviceResourceResolver = getResourceResolver()) {
            if (serviceResourceResolver == null) {
                LOG.warn("ResourceResolver is null, data feed to resources conversion is stopped");
                return Collections.emptyList();
            }
            List<Resource> items = toSlingResources(dataFeedToGridResources(serviceResourceResolver),
                    resourceResolverFactory.getThreadResourceResolver());
            LOG.info("Exadel Link Checker - the number of items shown on UI is {}", items.size());
            return items;
        }
    }

    private ResourceResolver getResourceResolver() {
        try {
            return resourceResolverFactory.getServiceResourceResolver(
                    ImmutableMap.of(ResourceResolverFactory.SUBSERVICE, LINK_CHECKER_SERVICE_NAME));
        } catch (LoginException e) {
            LOG.error("Failed to get service resource resolver", e);
        }
        return null;
    }

    private List<GridResource> dataFeedToGridResources(ResourceResolver resourceResolver) {
        Set<GridResource> gridResources = new HashSet<>();
        JSONArray jsonArray = JsonUtil.getJsonArrayFromFile(JSON_FILE_PATH, resourceResolver);
        int allItemsSize = jsonArray.length();
        if (allItemsSize > 0) {
            for (int i = 0; i < Math.min(allItemsSize, UI_ITEMS_LIMIT); i++) {
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
            if (resourcesJsonArray.length() > 0) {
                removePreviousDataFeed(resourceResolver);
                saveGridResourcesToJcr(resourceResolver, resourcesJsonArray);
                resourceResolver.commit();
                LOG.debug("Saving data feed json in jcr completed");
            }
        } catch (PersistenceException e) {
            LOG.error("Saving data feed json in jcr failed", e);
        }
    }

    private void removePreviousDataFeed(ResourceResolver resourceResolver) throws PersistenceException {
        LinkCheckerResourceUtil.removeResource(JSON_FILE_PATH, resourceResolver);
    }

    private void saveGridResourcesToJcr(ResourceResolver resourceResolver, JSONArray jsonArray) {
        LinkCheckerResourceUtil.saveFileToJCR(JSON_FILE_PATH, jsonArray.toString().getBytes(),
                ContentTypeUtil.TYPE_JSON, resourceResolver);
    }

    private List<Resource> toSlingResources(Collection<GridResource> gridResources, ResourceResolver resourceResolver) {
        return gridResources.stream()
                .map(gridResource -> toSlingResource(gridResource, resourceResolver))
                .collect(Collectors.toList());
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
}