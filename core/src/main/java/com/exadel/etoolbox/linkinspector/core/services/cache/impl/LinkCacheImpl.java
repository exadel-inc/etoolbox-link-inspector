package com.exadel.etoolbox.linkinspector.core.services.cache.impl;

import com.exadel.etoolbox.linkinspector.core.services.cache.LinkCache;
import com.exadel.etoolbox.linkinspector.core.services.data.models.GridResource;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

@Component(service = LinkCache.class)
public class LinkCacheImpl implements LinkCache {

    @SuppressWarnings("UnstableApiUsage")
    private Cache<String, CopyOnWriteArrayList<GridResource>> gridResourcesCache;

    @Activate
    @SuppressWarnings("unused")
    private void activate() {
        gridResourcesCache = CacheBuilder.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(100000, TimeUnit.DAYS)
                .build();
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public CopyOnWriteArrayList<GridResource> getGridResourcesList() {
        return gridResourcesCache.asMap().get("links");
    }
}
