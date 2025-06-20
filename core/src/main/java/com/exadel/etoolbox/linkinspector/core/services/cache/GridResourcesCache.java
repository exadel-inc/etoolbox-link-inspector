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

package com.exadel.etoolbox.linkinspector.core.services.cache;

import com.exadel.etoolbox.linkinspector.core.services.data.models.GridResource;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service interface for caching generated link inspection data.
 * <p>
 * This interface provides methods to manage a cache of {@link GridResource} objects,
 * allowing for efficient access to link inspection results without requiring regeneration
 * on each request. The implementation uses a thread-safe collection to ensure proper
 * concurrent access.
 */
public interface GridResourcesCache {

    /**
     * Retrieves the list of cached GridResource objects.
     *
     * @return Thread-safe list of GridResource objects representing link inspection data
     */
    CopyOnWriteArrayList<GridResource> getGridResourcesList();

    /**
     * Updates the cache with a new list of GridResource objects.
     *
     * @param gridResources The list of GridResource objects to store in the cache
     */
    void setGridResourcesList(List<GridResource> gridResources);

    /**
     * Clears the cache, removing all stored GridResource objects.
     * This is typically used when invalidating the cache due to content changes
     * or when manually triggering a refresh of link inspection data.
     */
    void clearCache();
}
