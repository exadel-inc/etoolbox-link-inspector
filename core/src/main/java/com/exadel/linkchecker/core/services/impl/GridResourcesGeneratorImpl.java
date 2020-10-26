package com.exadel.linkchecker.core.services.impl;

import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.exadel.linkchecker.core.models.GridResource;
import com.exadel.linkchecker.core.models.Link;
import com.exadel.linkchecker.core.services.GridResourcesGenerator;
import com.exadel.linkchecker.core.services.LinkHelper;
import com.exadel.linkchecker.core.services.util.GridResourceProperties;
import com.exadel.linkchecker.core.services.util.LinksCounter;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component(service = GridResourcesGenerator.class)
@Designate(ocd = GridResourcesGeneratorImpl.Configuration.class)
public class GridResourcesGeneratorImpl implements GridResourcesGenerator {
    @ObjectClassDefinition(
            name = "Exadel Link Checker - Grid Resources Generator",
            description = "Finds broken links below the specified path for further outputting them in a report"
    )
    @interface Configuration {
        @AttributeDefinition(
                name = "Path",
                description = "The content path for searching broken links. The search path should be located under /content"
        ) String search_path() default DEFAULT_SEARCH_PATH;

        @AttributeDefinition(
                name = "Excluded properties",
                description = "The List of properties excluded from processing"
        ) String[] excluded_properties() default {
                "dam:Comments",
                "cq:allowedTemplates",
                "cq:childrenOrder",
                "cq:designPath",
                "cq:lastModifiedBy",
                "cq:lastPublishedBy",
                "cq:lastReplicatedBy",
                "cq:lastReplicationAction",
                "cq:lastReplicationStatus",
                "cq:lastRolledoutBy",
                "cq:template",
                "jcr:createdBy",
                "sling:resourceType",
                "sling:resourceSuperType",
        };

        @AttributeDefinition(
                name = "Threads per core",
                description = "The number of threads created per each CPU core for validating links in parallel"
        ) int threads_per_core() default DEFAULT_THREADS_PER_CORE;
    }

    private static final Logger LOG = LoggerFactory.getLogger(GridResourcesGenerator.class);

    private static final String DEFAULT_SEARCH_PATH = "/content";
    private static final int DEFAULT_THREADS_PER_CORE = 4;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private LinkHelper linkHelper;

    private String searchPath;
    private String[] excludedProperties;
    private int threadsPerCore;

    @Activate
    @Modified
    protected void activate(Configuration configuration) {
        searchPath = configuration.search_path();
        excludedProperties = PropertiesUtil.toStringArray(configuration.excluded_properties());
        threadsPerCore = configuration.threads_per_core();
    }

    public List<Resource> generateGridResources(String gridResourceType) {
        StopWatch stopWatch = StopWatch.createStarted();
        LOG.info("Start link checker report generation");

        ResourceResolver resourceResolver = resourceResolverFactory.getThreadResourceResolver();
        if (resourceResolver == null) {
            LOG.warn("ResourceResolver is null, link checker report generation is stopped");
            return Collections.emptyList();
        }

        Resource rootResource = resourceResolver.getResource(searchPath);
        if (rootResource == null) {
            LOG.warn("Search path resource is null, link checker report generation is stopped");
            return Collections.emptyList();
        }

        Map<Link, List<GridResource>> linkToGridResourcesMap = new HashMap<>();
        int traversedNodesCounter = getGridResourcesViaTraversing(rootResource, gridResourceType, linkToGridResourcesMap);
        LOG.debug("Traversal is completed in {} ms, traversed nodes count: {}",
                stopWatch.getTime(TimeUnit.MILLISECONDS), traversedNodesCounter);

        Set<GridResource> gridResources = validateLinksInParallel(linkToGridResourcesMap, resourceResolver);
        List<Resource> itemsToShow = gridResources.stream()
                .map(gridResource -> createSlingResource(gridResource, resourceResolver))
                .collect(Collectors.toList());

        stopWatch.stop();
        LOG.info("Generating link checker report is completed in {} ms, the number of items is {}",
                stopWatch.getTime(TimeUnit.MILLISECONDS), itemsToShow.size());

        return itemsToShow;
    }

    private Set<GridResource> validateLinksInParallel(Map<Link, List<GridResource>> linkToGridResourcesMap,
                                                      ResourceResolver resourceResolver) {
        LinksCounter linksCounter = new LinksCounter();
        ExecutorService executorService =
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * threadsPerCore);
        Set<GridResource> gridResources = new CopyOnWriteArraySet<>();
        linkToGridResourcesMap.forEach((link, resources) -> {
                    linksCounter.countValidatedLinks(link);
                    executorService.submit(() -> {
                                if (!linkHelper.validateLink(link, resourceResolver)) {
                                    resources.forEach(resource -> resource.setLink(link));
                                    gridResources.addAll(resources);
                                }
                            }
                    );
                }
        );
        try {
            executorService.shutdown();
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            LOG.error("Parallel links validation failed", e);
        }
        LOG.debug("Checked internal links count: {}", linksCounter.getInternalLinks());
        LOG.debug("Checked external links count: {}", linksCounter.getExternalLinks());
        return gridResources;
    }

    private int getGridResourcesViaTraversing(Resource resource, String gridResourceType,
                                              Map<Link, List<GridResource>> allLinkToGridResourcesMap) {
        int traversedNodesCount = 0;
        traversedNodesCount++;
        getLinkToGridResourcesMap(resource, gridResourceType).forEach((k, v) ->
                allLinkToGridResourcesMap.merge(k, v,
                        (existing, newValue) -> {
                            existing.addAll(newValue);
                            return existing;
                        })
        );
        Iterator<Resource> children = resource.listChildren();
        while (children.hasNext()) {
            Resource child = children.next();
            traversedNodesCount += getGridResourcesViaTraversing(child, gridResourceType, allLinkToGridResourcesMap);
        }
        return traversedNodesCount;
    }

    private Map<Link, List<GridResource>> getLinkToGridResourcesMap(Resource resource, String gridResourceType) {
        return ResourceUtil.getValueMap(resource)
                .entrySet()
                .stream()
                .filter(valueMapEntry -> isNonExcludedProperty(valueMapEntry.getKey()))
                .flatMap(valueMapEntry ->
                        getLinkToGridResourceMap(valueMapEntry.getKey(), valueMapEntry.getValue(), resource, gridResourceType))
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList()))
                );
    }

    private Stream<Map.Entry<Link, GridResource>> getLinkToGridResourceMap(String property, Object propertyValue,
                                                                           Resource resource, String gridResourceType) {
        return linkHelper.getLinkStream(propertyValue)
                .collect(Collectors.toMap(Function.identity(),
                        link -> new GridResource(resource.getPath(), property, gridResourceType),
                        (existingValue, newValue) -> existingValue
                ))
                .entrySet()
                .stream();
    }

    private boolean isNonExcludedProperty(String propertyName) {
        //java.util.stream.Stream.noneMatch is not used to avoid Stream creation upon each property check
        for (String excludedProperty : excludedProperties) {
            if (excludedProperty.equals(propertyName)) {
                return false;
            }
        }
        return true;
    }

    private Resource createSlingResource(GridResource gridResource, ResourceResolver resourceResolver) {
        ValueMap valueMap = new ValueMapDecorator(new HashMap<>());
        valueMap.put(GridResourceProperties.PN_LINK, gridResource.getLink().getHref());
        valueMap.put(GridResourceProperties.PN_LINK_TYPE, gridResource.getLink().getType().getValue());
        valueMap.put(GridResourceProperties.PN_LINK_STATUS_CODE, gridResource.getLink().getStatusCode());
        valueMap.put(GridResourceProperties.PN_LINK_STATUS_MESSAGE, gridResource.getLink().getStatusMessage());
        valueMap.put(GridResourceProperties.PN_RESOURCE_PATH, gridResource.getResourcePath());
        valueMap.put(GridResourceProperties.PN_PROPERTY_NAME, gridResource.getPropertyName());
        return new ValueMapResource(resourceResolver, gridResource.getResourcePath(), gridResource.getResourceType(), valueMap);
    }
}