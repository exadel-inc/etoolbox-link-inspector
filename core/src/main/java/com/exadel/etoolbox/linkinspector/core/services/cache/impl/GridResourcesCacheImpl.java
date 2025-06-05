package com.exadel.etoolbox.linkinspector.core.services.cache.impl;

import com.exadel.etoolbox.linkinspector.core.services.cache.GridResourcesCache;
import com.exadel.etoolbox.linkinspector.core.services.data.models.GridResource;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Implements {@link GridResourcesCache} interface to provide data caching functionality.
 */
@Component(service = GridResourcesCache.class)
public class GridResourcesCacheImpl implements GridResourcesCache {

    private static final String BROKEN_LINKS_MAP_KEY = "elc-broken-links";

    private ConcurrentHashMap<String, CopyOnWriteArrayList<GridResource>> gridResourcesCache;

    @Activate
    @SuppressWarnings("unused")
    private void activate() {
        gridResourcesCache = new ConcurrentHashMap<>();
    }

    @Override
    public synchronized void setGridResourcesList(List<GridResource> gridResources) {
        gridResourcesCache.put(BROKEN_LINKS_MAP_KEY, new CopyOnWriteArrayList<>(gridResources));
    }

    @Override
    public void clearCache() {
        gridResourcesCache.clear();
    }

    @Override
    public CopyOnWriteArrayList<GridResource> getGridResourcesList() {
        return gridResourcesCache.getOrDefault(BROKEN_LINKS_MAP_KEY, new CopyOnWriteArrayList<>());
    }
}
