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

    /**
     * Updates the cache with a new list of GridResource objects.
     * This method is synchronized to ensure thread safety when updating the cache.
     *
     * @param gridResources The list of GridResource objects to store in the cache
     */
    @Override
    public synchronized void setGridResourcesList(List<GridResource> gridResources) {
        gridResourcesCache.put(BROKEN_LINKS_MAP_KEY, new CopyOnWriteArrayList<>(gridResources));
    }

    /**
     * Clears the cache, removing all stored GridResource objects.
     * This method is typically called when invalidating the cache due to content changes
     * or when manually triggering a refresh of link inspection data.
     */
    @Override
    public void clearCache() {
        gridResourcesCache.clear();
    }

    /**
     * Retrieves the list of cached GridResource objects.
     * If no data is found for the predefined key, returns an empty list.
     *
     * @return Thread-safe list of GridResource objects representing link inspection data
     */
    @Override
    public CopyOnWriteArrayList<GridResource> getGridResourcesList() {
        return gridResourcesCache.getOrDefault(BROKEN_LINKS_MAP_KEY, new CopyOnWriteArrayList<>());
    }
}
