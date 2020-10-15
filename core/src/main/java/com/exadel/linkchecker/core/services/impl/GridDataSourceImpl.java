package com.exadel.linkchecker.core.services.impl;

import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.exadel.linkchecker.core.services.GridDataSource;
import com.adobe.granite.ui.components.ds.DataSource;
import com.exadel.linkchecker.core.services.util.GridResourceProperties;
import com.exadel.linkchecker.core.services.LinkHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.exadel.linkchecker.core.models.Link;

import javax.jcr.query.Query;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component(
        service = GridDataSource.class
)
public class GridDataSourceImpl implements GridDataSource {
    private static final Logger LOG = LoggerFactory.getLogger(GridDataSource.class);

    private static final String PN_DATASOURCE = "datasource";
    private static final String PN_ITEM_RESOURCE_TYPE = "itemResourceType";

    //todo - search /content at the beginning of a property or "/content in the middle
    private static final String QUERY = "SELECT * FROM [nt:base] AS s WHERE ISDESCENDANTNODE([%s]) and (CONTAINS(s.*, 'http://') OR CONTAINS(s.*, 'https://') OR CONTAINS(s.*, 'www.') OR CONTAINS(s.*, '/content/'))";

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private LinkHelper linkHelper;

    @Override
    public DataSource getDataSource(HttpServletRequest request, Object cmp, Resource resource) {
        LOG.debug("Start link checker report generation");
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        ResourceResolver resourceResolver = resourceResolverFactory.getThreadResourceResolver();
        if (resourceResolver == null) {
            LOG.warn("ResourceResolver is null, link checker report generation is stopped");
            return new SimpleDataSource(Collections.emptyIterator());
        }

        //todo - make path configurable
        String query = String.format(QUERY, "/content");
        final Iterator<Resource> iterator = resourceResolver.findResources(query, Query.JCR_SQL2);
        LOG.debug("Query execution completed in {} ms", stopWatch.getTime(TimeUnit.MILLISECONDS));

        String gridResourceType = getDataSourceResourceType(resource);
        List<Resource> itemsToShow = getGridResources(iterator, gridResourceType);
        stopWatch.stop();
        LOG.debug("Generating link checker report is completed in {} ms, the nymber of items is {}",
                stopWatch.getTime(TimeUnit.MILLISECONDS), itemsToShow.size());

        return new SimpleDataSource(itemsToShow.iterator());
    }

    private List<Resource> getGridResources(Iterator<Resource> resourceIterator, String gridResourceType) {
        List<Resource> itemsToShow = new ArrayList<>();
        resourceIterator.forEachRemaining(res ->
                Optional.of(getGridResources(res, gridResourceType))
                        .filter(CollectionUtils::isNotEmpty)
                        .ifPresent(itemsToShow::addAll)
        );
        return itemsToShow;
    }

    private List<Resource> getGridResources(Resource resource, String gridResourceType) {
        return ResourceUtil.getValueMap(resource)
                .entrySet()
                .stream()
                .flatMap(entry -> propertyToGridResources(entry.getKey(), entry.getValue(), resource, gridResourceType))
                .collect(Collectors.toList());
    }

    private Stream<Resource> propertyToGridResources(String property, Object propertyValue, Resource resource,
                                                     String gridResourceType) {
        return linkHelper.getLinkStream(propertyValue, resource.getResourceResolver())
                .filter(link -> !link.isValid())
                .map(link -> createGridResource(link, property, resource, gridResourceType));
    }

    private Resource createGridResource(Link link, String propertyName, Resource resource, String gridResourceType) {
        ValueMap valueMap = new ValueMapDecorator(new HashMap<>());
        valueMap.put(GridResourceProperties.PN_LINK, link.getHref());
        valueMap.put(GridResourceProperties.PN_LINK_TYPE, link.getType());
        valueMap.put(GridResourceProperties.PN_LINK_STATUS_CODE, link.getStatusCode());
        valueMap.put(GridResourceProperties.PN_LINK_STATUS_MESSAGE, link.getStatusMessage());
        valueMap.put(GridResourceProperties.PN_RESOURCE_PATH, resource.getPath());
        valueMap.put(GridResourceProperties.PN_PROPERTY_NAME, propertyName);
        return new ValueMapResource(resource.getResourceResolver(), resource.getPath(), gridResourceType, valueMap);
    }

    private String getDataSourceResourceType(Resource resource) {
        return Optional.ofNullable(resource.getChild(PN_DATASOURCE))
                .map(ResourceUtil::getValueMap)
                .map(valueMap -> valueMap.get(PN_ITEM_RESOURCE_TYPE, String.class))
                .orElse(StringUtils.EMPTY);
    }
}
