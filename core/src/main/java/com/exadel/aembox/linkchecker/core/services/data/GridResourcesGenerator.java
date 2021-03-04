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

package com.exadel.aembox.linkchecker.core.services.data;

import com.exadel.aembox.linkchecker.core.services.data.models.GridResource;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.List;

public interface GridResourcesGenerator {
    /**
     * Collects broken links and builds the list of models {@link GridResource} for each link.
     * The model encloses all necessary data for saving it in the data feed and further usage in the Link Checker grid.     *
     * Each model instance contains data for a single row in the grid.
     *
     * @param gridResourceType - the resource type of items displayed in the Link Checker grid
     * @param resourceResolver - {@link ResourceResolver}
     * @return Set of models {@link GridResource}
     */
    List<GridResource> generateGridResources(String gridResourceType, ResourceResolver resourceResolver);
}