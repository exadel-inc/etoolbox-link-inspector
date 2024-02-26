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
import java.util.Map;

/**
 * Provides the service responsible for managing the data feed based on the set of resources generated
 * by ${@link GridResourcesGenerator}
 */
public interface DataFeedService {
    /**
     * If the node presents, users are informed that data feed regeneration is required
     * in order to display up-to-date results.
     */
    String PENDING_GENERATION_NODE = "/content/etoolbox-link-inspector/data/pendingDataFeedUpdate";

    String CSV_REPORT_NODE_PATH = "/content/etoolbox-link-inspector/data/content";

    /**
     * Collects broken links and generates json data feed for further usage in the Link Inspector grid.
     */
    void generateDataFeed();

    /**
     * Parses the data feed to the list of resources({@link Resource}) for further adapting them to view models
     * and displaying in the Link Inspector grid. The number of output items is limited for the sake of UX consistency.
     *
     * @param page - page number for displaying
     * @return the list of resources({@link Resource}) based on the data feed
     */
    List<Resource> dataFeedToResources(int page);

    /**
     * Parses the data feed to the list of models({@link GridResource}). The number of output items is not limited.
     *
     * @return the list of view items({@link GridResource}) based on the data feed
     */
    List<GridResource> dataFeedToGridResources(int page);

    /**
     * Process modification the data feed with new values from Map of link location and link new value.
     *
     * @param propertyLocationLinkMap - {@link Map} of link location and link new value
     * @param page                    - page number of report for modification
     */
    void modifyDataFeed(Map<String, String> propertyLocationLinkMap, int page);
}