package com.exadel.linkchecker.core.services.data.impl;

import com.day.cq.dam.api.DamConstants;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationStatus;
import com.day.cq.wcm.api.NameConstants;
import com.day.crx.JcrConstants;
import com.exadel.linkchecker.core.models.LinkStatus;
import com.exadel.linkchecker.core.services.data.models.GridResource;
import com.exadel.linkchecker.core.models.Link;
import com.exadel.linkchecker.core.services.data.GridResourcesGenerator;
import com.exadel.linkchecker.core.services.helpers.LinkHelper;
import com.exadel.linkchecker.core.services.util.LinksCounter;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.osgi.PropertiesUtil;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
                name = "Excluded paths",
                description = "The list of paths excluded from processing. The specified path and all its children " +
                        "are excluded. The excluded path should not end with slash. Can be specified as a regex"
        ) String[] excluded_paths() default {};

        @AttributeDefinition(
                name = "Activated Content",
                description = "If checked, links fill be retrieved from replicated (Activate) content only"
        ) boolean check_activation() default false;

        @AttributeDefinition(
                name = "Last Modified",
                description = "The content modified before the specified date will be excluded. " +
                        "Tha date should has the ISO-like date-time format, such as '2011-12-03T10:15:30+01:00'"
        ) String last_modified_boundary() default StringUtils.EMPTY;

        @AttributeDefinition(
                name = "Excluded properties",
                description = "The list of properties excluded from processing. Each value can be specified as a regex"
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
                name = "Links type",
                description = "The type of links in the report",
                options = {
                        @Option(label = "Internal", value = "INTERNAL"),
                        @Option(label = "External", value = "EXTERNAL"),
                        @Option(label = "Internal + External", value = StringUtils.EMPTY),
                }
        )
        String links_type() default StringUtils.EMPTY;

        @AttributeDefinition(
                name = "Excluded links patterns",
                description = "Links are excluded from processing if match any of the specified regex patterns"
        ) String[] excluded_links_patterns() default {};

        @AttributeDefinition(
                name = "Exclude tags",
                description = "If checked, the internal links starting with /content/cq:tags will be excluded"
        ) boolean exclude_tags() default true;

        @AttributeDefinition(
                name = "Status codes",
                description = "The list of status codes allowed for broken links in the report. " +
                        "Set a single negative value to allow all http error codes"
        )
        int[] allowed_status_codes() default {
                HttpStatus.SC_NOT_FOUND
        };

        @AttributeDefinition(
                name = "Threads per core",
                description = "The number of threads created per each CPU core for validating links in parallel"
        ) int threads_per_core() default DEFAULT_THREADS_PER_CORE;
    }

    private static final Logger LOG = LoggerFactory.getLogger(GridResourcesGenerator.class);

    private static final String DEFAULT_SEARCH_PATH = "/content";
    private static final String TAGS_LOCATION = "/content/cq:tags";
    private static final int DEFAULT_THREADS_PER_CORE = 60;

    @Reference
    private LinkHelper linkHelper;

    private ExecutorService executorService;

    private String searchPath;
    private String[] excludedPaths;
    private boolean checkActivation;
    private Instant lastModifiedBoundary;
    private String[] excludedProperties;
    private Link.Type reportLinksType;
    private String[] excludedLinksPatterns;
    private boolean excludeTags;
    private int[] allowedStatusCodes;
    private int threadsPerCore;

    @Activate
    @Modified
    protected void activate(Configuration configuration) {
        searchPath = configuration.search_path();
        excludedPaths = PropertiesUtil.toStringArray(configuration.excluded_paths());
        checkActivation = configuration.check_activation();
        lastModifiedBoundary = Optional.of(configuration.last_modified_boundary())
                .filter(StringUtils::isNotBlank)
                .map(dateString -> ZonedDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME))
                .map(ZonedDateTime::toInstant)
                .orElse(null);
        excludedProperties = PropertiesUtil.toStringArray(configuration.excluded_properties());
        reportLinksType = Optional.of(configuration.links_type())
                .filter(StringUtils::isNotBlank)
                .map(Link.Type::valueOf)
                .orElse(null);
        excludedLinksPatterns = PropertiesUtil.toStringArray(configuration.excluded_links_patterns());
        excludeTags = configuration.exclude_tags();
        allowedStatusCodes = configuration.allowed_status_codes();
        threadsPerCore = configuration.threads_per_core();
    }

    @Override
    public List<GridResource> generateGridResources(String gridResourceType, ResourceResolver resourceResolver) {
        StopWatch stopWatch = StopWatch.createStarted();
        LOG.debug("Start broken links collecting, path: {}", searchPath);

        Resource rootResource = resourceResolver.getResource(searchPath);
        if (rootResource == null) {
            LOG.warn("Search path resource is null, link checker report generation is stopped");
            return Collections.emptyList();
        }

        Map<Link, List<GridResource>> linkToGridResourcesMap = new HashMap<>();
        int traversedNodesCounter = getGridResourcesViaTraversing(rootResource, gridResourceType, linkToGridResourcesMap);
        LOG.debug("Traversal is completed in {} ms, path: {}, traversed nodes count: {}",
                stopWatch.getTime(TimeUnit.MILLISECONDS), searchPath, traversedNodesCounter);

        if (linkToGridResourcesMap.isEmpty()) {
            LOG.warn("Collecting broken links is completed in {} ms, path: {}. No broken links were found after traversing",
                    stopWatch.getTime(TimeUnit.MILLISECONDS), searchPath);
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

    private Set<GridResource> validateLinksInParallel(Map<Link, List<GridResource>> linkToGridResourcesMap,
                                                      ResourceResolver resourceResolver) {
        LinksCounter linksCounter = new LinksCounter();
        LinksCounter brokenLinksCounter = new LinksCounter();
        executorService =
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * threadsPerCore);
        Set<GridResource> gridResources = new CopyOnWriteArraySet<>();
        try {
            linkToGridResourcesMap.forEach((link, resources) -> {
                        linksCounter.countValidatedLinks(link);
                        executorService.submit(() -> {
                                    LinkStatus status = linkHelper.validateLink(link, resourceResolver);
                                    if (!status.isValid() && isAllowedErrorCode(status.getStatusCode())) {
                                        resources.forEach(resource -> resource.setLink(link));
                                        gridResources.addAll(resources);
                                        brokenLinksCounter.countValidatedLinks(link);
                                    }
                                }
                        );
                    }
            );
        } finally {
            executorService.shutdown();
        }
        try {
            boolean terminated = executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            LOG.trace("ExecutorService terminated: {}", terminated);
        } catch (InterruptedException e) {
            LOG.error("Parallel links validation failed", e);
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOG.debug("Checked internal links count: {}", linksCounter.getInternalLinks());
        LOG.debug("Checked external links count: {}", linksCounter.getExternalLinks());

        LOG.debug("Broken internal links count: {}", brokenLinksCounter.getInternalLinks());
        LOG.debug("Broken external links count: {}", brokenLinksCounter.getExternalLinks());
        return gridResources;
    }

    private int getGridResourcesViaTraversing(Resource resource, String gridResourceType,
                                              Map<Link, List<GridResource>> allLinkToGridResourcesMap) {
        int traversedNodesCount = 0;
        if (isAllowedResource(resource)) {
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

    private Stream<Map.Entry<Link, GridResource>> getLinkToGridResourceMap(String property, Object propertyValue,
                                                                           Resource resource, String gridResourceType) {
        return linkHelper.getLinkStreamFromProperty(propertyValue)
                .filter(this::isAllowedLinkType)
                .filter(this::isAllowedLink)
                .collect(Collectors.toMap(Function.identity(),
                        link -> new GridResource(resource.getPath(), property, gridResourceType),
                        (existingValue, newValue) -> existingValue
                ))
                .entrySet()
                .stream();
    }

    private boolean isAllowedResource(Resource resource) {
        return !isExcludedPath(resource.getPath()) && isAllowedReplicationStatus(resource)
                && isAllowedLastModifiedDate(resource);
    }

    private boolean isAllowedLastModifiedDate(Resource resource) {
        if (lastModifiedBoundary == null) {
            return true;
        }
        ValueMap properties = resource.getValueMap();
        Date cqLastModified = properties.get(NameConstants.PN_PAGE_LAST_MOD, Date.class);
        Date jcrLastModified = properties.get(JcrConstants.JCR_LASTMODIFIED, Date.class);

        return Stream.of(cqLastModified, jcrLastModified)
                .filter(Objects::nonNull)
                .sorted(Comparator.reverseOrder())
                .map(Date::toInstant)
                .findFirst()
                .map(lastModifiedBoundary::isBefore)
                .orElse(true);
    }

    private boolean isAllowedLinkType(Link link) {
        return reportLinksType == null || reportLinksType == link.getType();
    }

    private boolean isAllowedLink(Link link) {
        return !(Link.Type.INTERNAL == link.getType() && isExcludedTag(link.getHref())) &&
                !isExcludedByPattern(link.getHref());
    }

    private boolean isExcludedByPattern(String href) {
        return isStringMatchAnyPattern(href, excludedLinksPatterns);
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
            boolean isPageOrAsset = Optional.of(resource.getValueMap())
                    .map(valueMap -> valueMap.get(JcrConstants.JCR_PRIMARYTYPE))
                    .filter(type -> NameConstants.NT_PAGE.equals(type) || DamConstants.NT_DAM_ASSET.equals(type))
                    .isPresent();
            if (isPageOrAsset) {
                return isActivatedResource(resource);
            } else {
                Optional<String> replicationAction = Optional.of(resource.getValueMap())
                        .map(valueMap -> valueMap.get(ReplicationStatus.NODE_PROPERTY_LAST_REPLICATION_ACTION, String.class));
                if (replicationAction.isPresent()) {
                    return replicationAction.filter(ReplicationActionType.ACTIVATE.getName()::equals)
                            .isPresent();
                }
            }
        }
        return true;
    }

    private boolean isActivatedResource(Resource resource) {
        return Optional.ofNullable(resource.adaptTo(ReplicationStatus.class))
                .filter(ReplicationStatus::isActivated)
                .isPresent();
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

    @Deactivate
    protected void deactivate() {
        LOG.debug("Deactivate GridResourcesGenerator (executorService.shutdownNow)");
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }
}