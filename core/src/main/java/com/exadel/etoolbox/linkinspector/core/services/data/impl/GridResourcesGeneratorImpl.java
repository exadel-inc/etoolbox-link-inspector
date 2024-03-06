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

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationStatus;
import com.day.crx.JcrConstants;
import com.exadel.etoolbox.linkinspector.core.models.Link;
import com.exadel.etoolbox.linkinspector.core.models.LinkStatus;
import com.exadel.etoolbox.linkinspector.core.services.data.GenerationStatsProps;
import com.exadel.etoolbox.linkinspector.core.services.data.GridResourcesGenerator;
import com.exadel.etoolbox.linkinspector.core.services.data.UiConfigService;
import com.exadel.etoolbox.linkinspector.core.services.helpers.LinkHelper;
import com.exadel.etoolbox.linkinspector.core.services.data.models.GridResource;
import com.exadel.etoolbox.linkinspector.core.services.util.LinkInspectorResourceUtil;
import com.exadel.etoolbox.linkinspector.core.services.util.LinksCounter;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implements {@link GridResourcesGenerator} interface to provide an OSGi service which is responsible for the data feed
 * generation and further adaptation the data feed to the models for building the UI grid
 */
@Component(service = GridResourcesGenerator.class)
@Designate(ocd = GridResourcesGeneratorImpl.Configuration.class)
public class GridResourcesGeneratorImpl implements GridResourcesGenerator {
    @ObjectClassDefinition(
            name = "EToolbox Link Inspector - Grid Resources Generator",
            description = "Finds broken links under the specified path for further outputting them in a report"
    )
    @interface Configuration {
        @AttributeDefinition(
                name = "Path",
                description = "The content path for searching broken links. The search path should be located under /content"
        ) String searchPath() default DEFAULT_SEARCH_PATH;

        @AttributeDefinition(
                name = "Excluded paths",
                description = "The list of paths excluded from processing. The specified path and all its children " +
                        "are excluded. The excluded path should not end with slash. Can be specified as a regex"
        ) String[] excludedPaths() default {};

        @AttributeDefinition(
                name = "Activated Content",
                description = "If checked, links will be retrieved from activated content only"
        ) boolean checkActivation() default false;

        @AttributeDefinition(
                name = "Skip content modified after activation",
                description = "Works in conjunction with the 'Activated Content' checkbox only. If checked, links " +
                        "will be retrieved from activated content that is not modified after activation " +
                        "(lastModified is before lastReplicated)"
        ) boolean skipModifiedAfterActivation() default false;

        @AttributeDefinition(
                name = "Last Modified",
                description = "The content modified before the specified date will be excluded. " +
                        "Tha date should has the ISO-like date-time format, such as '2011-12-03T10:15:30+01:00'"
        ) String lastModifiedBoundary() default StringUtils.EMPTY;

        @AttributeDefinition(
                name = "Excluded properties",
                description = "The list of properties excluded from processing. Each value can be specified as a regex"
        ) String[] excludedProperties() default {
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
                name = "Links type",
                description = "The type of links in the report",
                options = {
                        @Option(label = "Internal", value = "INTERNAL"),
                        @Option(label = "External", value = "EXTERNAL"),
                        @Option(
                                label = GenerationStatsProps.REPORT_LINKS_TYPE_ALL,
                                value = GenerationStatsProps.REPORT_LINKS_TYPE_ALL
                        ),
                }
        )
        String linksType() default GenerationStatsProps.REPORT_LINKS_TYPE_ALL;

        @AttributeDefinition(
                name = "Excluded links patterns",
                description = "Links are excluded from processing if match any of the specified regex patterns"
        ) String[] excludedLinksPatterns() default {};

        @AttributeDefinition(
                name = "Exclude tags",
                description = "If checked, the internal links starting with /content/cq:tags will be excluded"
        ) boolean excludeTags() default true;

        @AttributeDefinition(
                name = "Status codes",
                description = "The list of status codes allowed for broken links in the report. " +
                        "Set a single negative value to allow all http error codes"
        )
        int[] allowedStatusCodes() default {
                HttpStatus.SC_NOT_FOUND
        };

        @AttributeDefinition(
                name = "Threads per core",
                description = "The number of threads created per each CPU core for validating links in parallel"
        ) int threadsPerCore() default DEFAULT_THREADS_PER_CORE;
    }

    private static final Logger LOG = LoggerFactory.getLogger(GridResourcesGeneratorImpl.class);

    private static final String DEFAULT_SEARCH_PATH = "/content";
    private static final int DEFAULT_THREADS_PER_CORE = 60;

    private static final String TAGS_LOCATION = "/content/cq:tags";
    private static final String STATS_RESOURCE_PATH = "/content/etoolbox-link-inspector/data/stats";

    @Reference
    private LinkHelper linkHelper;
    @Reference
    private UiConfigService uiConfigService;

    private ExecutorService executorService;

    private String searchPath;
    private String[] excludedPaths;
    private boolean checkActivation;
    private boolean skipModifiedAfterActivation;
    private ZonedDateTime lastModifiedBoundary;
    private String[] excludedProperties;
    private String reportLinksType;
    private String[] excludedLinksPatterns;
    private boolean excludeTags;
    private int[] allowedStatusCodes;
    private int threadsPerCore;

    /**
     * Inits fields based on the service configuration
     * @param configuration - the service configuration
     */
    @Activate
    @Modified
    protected void activate(Configuration configuration) {
        searchPath = uiConfigService.getSearchPath();
        excludedPaths = configuration.excludedPaths();
        checkActivation = configuration.checkActivation();
        skipModifiedAfterActivation = configuration.skipModifiedAfterActivation();
        lastModifiedBoundary = Optional.of(configuration.lastModifiedBoundary())
                .filter(StringUtils::isNotBlank)
                .map(dateString -> ZonedDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME))
                .orElse(null);
        excludedProperties = configuration.excludedProperties();
        reportLinksType = configuration.linksType();
        excludedLinksPatterns = configuration.excludedLinksPatterns();
        excludeTags = configuration.excludeTags();
        allowedStatusCodes = configuration.allowedStatusCodes();
        threadsPerCore = configuration.threadsPerCore();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<GridResource> generateGridResources(String gridResourceType, ResourceResolver resourceResolver) {
        uiConfigService.getExcludedLinksPatterns();
        StopWatch stopWatch = StopWatch.createStarted();
        LOG.debug("Start broken links collecting, path: {}", searchPath);

        Resource rootResource = resourceResolver.getResource(searchPath);
        if (rootResource == null) {
            LOG.warn("Search path resource is null, link inspector report generation is stopped");
            return Collections.emptyList();
        }

        Map<Link, List<GridResource>> linkToGridResourcesMap = new HashMap<>();
        int traversedNodesCounter = getGridResourcesViaTraversing(rootResource, gridResourceType, linkToGridResourcesMap);
        LOG.debug("Traversal is completed in {} ms, path: {}, traversed nodes count: {}",
                stopWatch.getTime(TimeUnit.MILLISECONDS), searchPath, traversedNodesCounter);

        if (linkToGridResourcesMap.isEmpty()) {
            LOG.warn("Collecting broken links is completed in {} ms, path: {}. No broken links were found after traversing",
                    stopWatch.getTime(TimeUnit.MILLISECONDS), searchPath);
            LinksCounter emptyCounter = new LinksCounter();
            saveStatsToJcr(emptyCounter, emptyCounter, resourceResolver);
            return Collections.emptyList();
        }

        List<GridResource> sortedGridResources = validateLinksInParallel(linkToGridResourcesMap, resourceResolver)
                .stream()
                .sorted(Comparator.comparing(GridResource::getHref))
                .collect(Collectors.toList());

        stopWatch.stop();
        LOG.info("Collecting broken links is completed in {} ms, path: {}, the number of grid items is {}",
                stopWatch.getTime(TimeUnit.MILLISECONDS), searchPath, sortedGridResources.size());

        return sortedGridResources;
    }

    private int getGridResourcesViaTraversing(Resource resource,
                                              String gridResourceType,
                                              Map<Link, List<GridResource>> allLinkToGridResourcesMap) {
        int traversedNodesCount = 0;
        if (!isAllowedResource(resource)) {
            return traversedNodesCount;
        }
        getLinkToGridResourcesMap(resource, gridResourceType).forEach((k, v) ->
                allLinkToGridResourcesMap.merge(k, v,
                        (existing, newValue) -> {
                            existing.addAll(newValue);
                            return existing;
                        })
        );
        traversedNodesCount++;
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
                .filter(valueMapEntry -> !isExcludedProperty(valueMapEntry.getKey()))
                .flatMap(valueMapEntry ->
                        getLinkToGridResourceMap(valueMapEntry.getKey(), valueMapEntry.getValue(), resource, gridResourceType))
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList()))
                );
    }

    private Stream<Map.Entry<Link, GridResource>> getLinkToGridResourceMap(String property,
                                                                           Object propertyValue,
                                                                           Resource resource,
                                                                           String gridResourceType) {
        return linkHelper.getLinkStreamFromProperty(propertyValue)
                .filter(this::isAllowedLinkType)
                .filter(this::isAllowedLink)
                .collect(Collectors.toMap(Function.identity(),
                        link -> new GridResource(link, resource.getPath(), property, gridResourceType),
                        (existingValue, newValue) -> existingValue
                ))
                .entrySet()
                .stream();
    }

    private Set<GridResource> validateLinksInParallel(Map<Link, List<GridResource>> linkToGridResourcesMap,
                                                      ResourceResolver resourceResolver) {
        LinksCounter allLinksCounter = new LinksCounter();
        LinksCounter brokenLinksCounter = new LinksCounter();
        Set<GridResource> allBrokenLinkResources = new CopyOnWriteArraySet<>();
        try {
            executorService =
                    Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * threadsPerCore);
            linkToGridResourcesMap.forEach((link, resources) ->
                    submitLinkForValidation(
                            link,
                            resources,
                            allBrokenLinkResources,
                            allLinksCounter,
                            brokenLinksCounter,
                            resourceResolver
                    )
            );
        } finally {
            executorService.shutdown();
        }

        awaitExecutorServiceTermination();

        LOG.debug("Checked internal links count: {}", allLinksCounter.getInternalLinks());
        LOG.debug("Checked external links count: {}", allLinksCounter.getExternalLinks());

        LOG.debug("Broken internal links count: {}", brokenLinksCounter.getInternalLinks());
        LOG.debug("Broken external links count: {}", brokenLinksCounter.getExternalLinks());

        saveStatsToJcr(allLinksCounter, brokenLinksCounter, resourceResolver);

        return allBrokenLinkResources;
    }

    private void submitLinkForValidation(Link link,
                                         List<GridResource> currentLinkResources,
                                         Set<GridResource> allBrokenLinkResources,
                                         LinksCounter allLinksCounter,
                                         LinksCounter brokenLinksCounter,
                                         ResourceResolver resourceResolver) {
        allLinksCounter.countLink(link);
        executorService.submit(() -> {
                    LinkStatus status = linkHelper.validateLink(link, resourceResolver);
                    if (!status.isValid() && isAllowedErrorCode(status.getStatusCode())) {
                        allBrokenLinkResources.addAll(currentLinkResources);
                        brokenLinksCounter.countLink(link);
                    }
                }
        );
    }

    private void awaitExecutorServiceTermination() {
        try {
            boolean terminated = executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            LOG.trace("ExecutorService terminated: {}", terminated);
        } catch (InterruptedException e) {
            LOG.error("Parallel links validation failed", e);
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private boolean isAllowedResource(Resource resource) {
        return !isExcludedPath(resource.getPath()) && isAllowedReplicationStatus(resource)
                && isAllowedLastModifiedDate(resource);
    }

    private boolean isAllowedLastModifiedDate(Resource resource) {
        if (lastModifiedBoundary == null) {
            return true;
        }
        return Optional.ofNullable(LinkInspectorResourceUtil.getLastModified(resource))
                .map(lastModified -> lastModifiedBoundary.toInstant().isBefore(lastModified))
                .orElse(true);
    }

    private boolean isAllowedLinkType(Link link) {
        return GenerationStatsProps.REPORT_LINKS_TYPE_ALL.equals(reportLinksType) ||
                Link.Type.valueOf(reportLinksType) == link.getType();
    }

    private boolean isAllowedLink(Link link) {
        return !(Link.Type.INTERNAL == link.getType() && isExcludedTag(link.getHref())) &&
                !isExcludedByPattern(link.getHref());
    }

    private boolean isExcludedByPattern(String href) {
        return isStringMatchAnyPattern(href, getExcludedLinksPatterns());
    }

    private boolean isExcludedTag(String href) {
        return excludeTags && href.startsWith(TAGS_LOCATION);
    }

    private boolean isExcludedProperty(String propertyName) {
        return isStringMatchAnyPattern(propertyName, excludedProperties);
    }

    private boolean isExcludedPath(String path) {
        return isStringMatchAnyPattern(path, excludedPaths);
    }

    private boolean isAllowedErrorCode(int linkStatusCode) {
        if (ArrayUtils.isEmpty(allowedStatusCodes) ||
                (allowedStatusCodes.length == 1 && allowedStatusCodes[0] < 0)) {
            return true;
        }
        for (int allowedStatusCode : allowedStatusCodes) {
            if (allowedStatusCode == linkStatusCode) {
                return true;
            }
        }
        return false;
    }

    private boolean isAllowedReplicationStatus(Resource resource) {
        if (checkActivation) {
            if (LinkInspectorResourceUtil.isPageOrAsset(resource)) {
                return isActivatedPageOrAsset(resource);
            } else {
                Optional<String> replicationAction = Optional.of(resource.getValueMap())
                        .map(valueMap -> valueMap.get(ReplicationStatus.NODE_PROPERTY_LAST_REPLICATION_ACTION, String.class));
                if (replicationAction.isPresent()) {
                    return ReplicationActionType.ACTIVATE.getName().equals(replicationAction.get()) &&
                            (!skipModifiedAfterActivation || LinkInspectorResourceUtil.isModifiedBeforeActivation(resource));
                }
            }
        }
        return true;
    }

    private boolean isActivatedPageOrAsset(Resource pageOrAssetResource) {
        Optional<ReplicationStatus> replicationStatus =
                Optional.ofNullable(pageOrAssetResource.adaptTo(ReplicationStatus.class))
                        .filter(ReplicationStatus::isActivated);
        if (!replicationStatus.isPresent()) {
            return false;
        }
        return !skipModifiedAfterActivation || replicationStatus.map(ReplicationStatus::getLastPublished)
                .map(Calendar::toInstant)
                .map(instant -> isModifiedBeforeActivation(pageOrAssetResource, instant))
                .orElse(true);
    }

    private boolean isModifiedBeforeActivation(Resource pageOrAssetResource, Instant lastReplicated) {
        Resource resourceToCheck = Optional.ofNullable(pageOrAssetResource.getChild(JcrConstants.JCR_CONTENT))
                .orElse(pageOrAssetResource);
        Instant lastModified = LinkInspectorResourceUtil.getLastModified(resourceToCheck);
        return LinkInspectorResourceUtil.isModifiedBeforeActivation(lastModified, lastReplicated);
    }

    private boolean isStringMatchAnyPattern(String value, String[] patterns) {
        if (ArrayUtils.isEmpty(patterns)) {
            return false;
        }
        for (String pattern : patterns) {
            if (StringUtils.isNotBlank(pattern) && value.matches(pattern)) {
                return true;
            }
        }
        return false;
    }

    private void saveStatsToJcr(LinksCounter allLinksCounter, LinksCounter brokenLinksCounter, ResourceResolver resourceResolver) {
        try {
            LinkInspectorResourceUtil.removeResource(STATS_RESOURCE_PATH, resourceResolver);
            ResourceUtil.getOrCreateResource(
                    resourceResolver,
                    STATS_RESOURCE_PATH,
                    getGenerationStatsMap(allLinksCounter, brokenLinksCounter),
                    JcrResourceConstants.NT_SLING_FOLDER,
                    true
            );
        } catch (PersistenceException e) {
            LOG.error(String.format("Failed to create the resource %s", STATS_RESOURCE_PATH), e);
        }
    }

    private Map<String, Object> getGenerationStatsMap(LinksCounter allLinksCounter, LinksCounter brokenLinksCounter) {
        Map<String, Object> stats = new HashMap<>();

        stats.put(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, JcrConstants.NT_UNSTRUCTURED);

        stats.put(GenerationStatsProps.PN_LAST_GENERATED,
                ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME));
        stats.put(GenerationStatsProps.PN_SEARCH_PATH, searchPath);
        stats.put(GenerationStatsProps.PN_EXCLUDED_PATHS, excludedPaths);
        stats.put(GenerationStatsProps.PN_CHECK_ACTIVATION, checkActivation);
        stats.put(GenerationStatsProps.PN_SKIP_MODIFIED_AFTER_ACTIVATION, skipModifiedAfterActivation);
        stats.put(GenerationStatsProps.PN_LAST_MODIFIED_BOUNDARY, dateToIsoDateTimeString(lastModifiedBoundary));
        stats.put(GenerationStatsProps.PN_EXCLUDED_PROPERTIES, excludedProperties);

        stats.put(GenerationStatsProps.PN_REPORT_LINKS_TYPE, reportLinksType);
        stats.put(GenerationStatsProps.PN_EXCLUDED_LINK_PATTERNS, getExcludedLinksPatterns());
        stats.put(GenerationStatsProps.PN_EXCLUDED_TAGS, excludeTags);
        stats.put(GenerationStatsProps.PN_ALLOWED_STATUS_CODES, allowedStatusCodes);

        stats.put(GenerationStatsProps.PN_ALL_INTERNAL_LINKS, allLinksCounter.getInternalLinks());
        stats.put(GenerationStatsProps.PN_BROKEN_INTERNAL_LINKS, brokenLinksCounter.getInternalLinks());
        stats.put(GenerationStatsProps.PN_ALL_EXTERNAL_LINKS, allLinksCounter.getExternalLinks());
        stats.put(GenerationStatsProps.PN_BROKEN_EXTERNAL_LINKS, brokenLinksCounter.getExternalLinks());

        return stats;
    }

    private String dateToIsoDateTimeString(ZonedDateTime zonedDateTime) {
        return Optional.ofNullable(zonedDateTime)
                .map(date -> date.format(DateTimeFormatter.ISO_DATE_TIME))
                .orElse(StringUtils.EMPTY);
    }

    public String[] getExcludedLinksPatterns() {
        List<String> patterns = new ArrayList<>(Arrays.asList(excludedLinksPatterns));
        String[] uiPatterns = uiConfigService.getExcludedLinksPatterns();
        patterns.addAll(Arrays.asList(uiPatterns));
        patterns = patterns.stream().filter(p->{
            try {
                Pattern.compile(p);
            } catch (PatternSyntaxException exception) {
                LOG.warn("Excluded Links - Configured invalid regular expression: {}", p);
                return false;
            }
            return true;
        }).collect(Collectors.toList());
        return patterns.toArray(new String[excludedLinksPatterns.length + uiPatterns.length]);
    }

    @Deactivate
    protected void deactivate() {
        LOG.debug("Deactivate GridResourcesGenerator (executorService.shutdownNow)");
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }
}