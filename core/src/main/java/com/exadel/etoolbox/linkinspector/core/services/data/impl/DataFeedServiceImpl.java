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
import com.exadel.etoolbox.linkinspector.core.models.Link;
import com.exadel.etoolbox.linkinspector.core.models.LinkStatus;
import com.exadel.etoolbox.linkinspector.core.models.ui.GridViewItem;
import com.exadel.etoolbox.linkinspector.core.services.data.DataFeedService;
import com.exadel.etoolbox.linkinspector.core.services.data.GridResourcesGenerator;
import com.exadel.etoolbox.linkinspector.core.services.data.models.GridResource;
import com.exadel.etoolbox.linkinspector.core.services.helpers.CsvHelper;
import com.exadel.etoolbox.linkinspector.core.services.helpers.LinkHelper;
import com.exadel.etoolbox.linkinspector.core.services.helpers.RepositoryHelper;
import com.exadel.etoolbox.linkinspector.core.services.util.LinkInspectorResourceUtil;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
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

    @Reference
    private RepositoryHelper repositoryHelper;

    @Reference
    private GridResourcesGenerator gridResourcesGenerator;

    @Reference
    private CsvHelper csvHelper;

    @Reference
    private LinkHelper linkHelper;

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
            Optional.of(gridResourcesGenerator.generateGridResources(GRID_RESOURCE_TYPE, resourceResolver))
                    .ifPresent(gridResources -> {
                        generateCsvReport(gridResources, resourceResolver);
                        removePendingNode(resourceResolver);
                    });
            LOG.info("Link inspector data feed generation is completed");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Resource> dataFeedToResources(int page) {
        LOG.debug("Start data feed to resources conversion");
        try (ResourceResolver serviceResourceResolver = repositoryHelper.getServiceResourceResolver()) {
            if (serviceResourceResolver == null) {
                LOG.warn("ResourceResolver is null, data feed to resources conversion is stopped");
                return Collections.emptyList();
            }
            List<Resource> resources = toSlingResourcesStream(
                    csvHelper.readCsvReport(serviceResourceResolver, page),
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
    public List<GridResource> dataFeedToGridResources(int page) {
        try (ResourceResolver serviceResourceResolver = repositoryHelper.getServiceResourceResolver()) {
            if (serviceResourceResolver == null) {
                LOG.warn("ResourceResolver is null, data feed to grid resources conversion is stopped");
                return Collections.emptyList();
            }
            return dataFeedToGridResources(serviceResourceResolver, page);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void modifyDataFeed(Map<String, String> propertyLocationLinkMap, int page) {
        try (ResourceResolver serviceResourceResolver = repositoryHelper.getServiceResourceResolver()) {
            List<GridResource> updatedResources = readGridResources(serviceResourceResolver, page).stream().peek(resource -> {
                if (propertyLocationLinkMap.containsKey(resource.getPropertyLocation())) {
                    modifyLink(propertyLocationLinkMap, serviceResourceResolver, resource);
                }
            }).collect(Collectors.toList());
            List<GridViewItem> gridViewItems = toSlingResourcesStream(updatedResources, serviceResourceResolver)
                    .map(resource -> resource.adaptTo(GridViewItem.class))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            csvHelper.saveCsvReport(serviceResourceResolver, gridViewItems, page);
        }
    }

    private List<GridResource> dataFeedToGridResources(ResourceResolver resourceResolver, int page) {
        Resource resource = resourceResolver.getResource(CSV_REPORT_NODE_PATH);
        if (resource == null) {
            LOG.error("Resource {} doesn't exist.", CSV_REPORT_NODE_PATH);
            return Collections.emptyList();
        }
        return csvHelper.readCsvReport(resourceResolver, page);
    }

    private void removePendingNode(ResourceResolver resourceResolver) {
        LinkInspectorResourceUtil.removeResource(DataFeedService.PENDING_GENERATION_NODE, resourceResolver);
    }

    private Stream<Resource> toSlingResourcesStream(Collection<GridResource> gridResources, ResourceResolver resourceResolver) {
        return gridResources.stream()
                .map(gridResource -> toSlingResource(gridResource, resourceResolver));
    }

    private void generateCsvReport(Collection<GridResource> gridResources, ResourceResolver resourceResolver) {
        StopWatch stopWatch = StopWatch.createStarted();
        LOG.debug("Start CSV report generation");

        List<GridViewItem> gridViewItems = toSlingResourcesStream(gridResources, resourceResolver)
                .map(resource -> resource.adaptTo(GridViewItem.class))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        csvHelper.generateCsvReport(resourceResolver, gridViewItems);

        stopWatch.stop();
        LOG.debug("Generation of CSV report is completed in {} ms", stopWatch.getTime(TimeUnit.MILLISECONDS));
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

    private List<GridResource> readGridResources(ResourceResolver resourceResolver, int page) {
        return csvHelper.readCsvReport(resourceResolver, page);
    }

    private void modifyLink(Map<String, String> propertyLocationLinkMap, ResourceResolver resourceResolver, GridResource resource) {
        Optional<Link> optionalLink = linkHelper
                .getLinkStreamFromProperty(propertyLocationLinkMap.get(resource.getPropertyLocation()))
                .peek(link -> linkHelper.validateLink(link, resourceResolver))
                .findFirst();
        if (optionalLink.isPresent()) {
            Link link = optionalLink.get();
            link.setStatus(new LinkStatus(link.getStatusCode(), "Link Modified"));
            resource.setLink(link);
        }
    }
}