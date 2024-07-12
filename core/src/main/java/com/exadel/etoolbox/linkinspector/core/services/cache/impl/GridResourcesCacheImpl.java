package com.exadel.etoolbox.linkinspector.core.services.cache.impl;

import com.exadel.etoolbox.linkinspector.core.services.cache.GridResourcesCache;
import com.exadel.etoolbox.linkinspector.core.services.data.models.GridResource;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Implements {@link GridResourcesCache} interface to provide data caching functionality.
 */
@Component(service = GridResourcesCache.class)
public class GridResourcesCacheImpl implements GridResourcesCache {

    private static final String BROKEN_LINKS_MAP_KEY = "elc-broken-links";

    @SuppressWarnings("UnstableApiUsage")
    private Cache<String, CopyOnWriteArrayList<GridResource>> gridResourcesCache;

    @Activate
    @SuppressWarnings("unused")
    private void activate() {
        gridResourcesCache = CacheBuilder.newBuilder()
                .maximumSize(1)
                .expireAfterWrite(100000, TimeUnit.DAYS)
                .build();
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public synchronized void setGridResourcesList(List<GridResource> gridResources) {
        gridResourcesCache.asMap().put(BROKEN_LINKS_MAP_KEY, new CopyOnWriteArrayList<>(gridResources));
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public CopyOnWriteArrayList<GridResource> getGridResourcesList() {
        return gridResourcesCache.asMap().getOrDefault(BROKEN_LINKS_MAP_KEY, new CopyOnWriteArrayList<>());
    }
}
