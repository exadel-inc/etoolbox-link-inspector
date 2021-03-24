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

package com.exadel.etoolbox.linkinspector.core.services.data;

import com.exadel.etoolbox.linkinspector.core.services.data.models.GridResource;
import org.apache.sling.api.resource.Resource;

import java.util.List;

public interface DataFeedService {
    /**
     * If the node presents, users are informed that data feed regeneration is required
     * in order to display up-to-date results.
     */
    String PENDING_GENERATION_NODE = "/content/etoolbox-link-inspector/data/pendingDataFeedUpdate";

    /**
     * Collects broken links and generates json data feed for further usage in the Link Inspector grid.
     */
    void generateDataFeed();

    /**
     * Parses the data feed to the list of resources({@link Resource}) for further adapting to view models
     * and displaying them in the Link Inspector grid. The number of output items is limited.
     *
     * @return the list of resources({@link Resource}) based on the data feed
     */
    List<Resource> dataFeedToResources();

    /**
     * Parses the data feed to the list of models({@link GridResource}). The number of output items is not limited.
     *
     * @return the list of view items({@link GridResource}) based on the data feed
     */
    List<GridResource> dataFeedToGridResources();
}