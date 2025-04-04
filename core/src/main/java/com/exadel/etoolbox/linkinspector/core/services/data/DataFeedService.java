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

import com.exadel.etoolbox.linkinspector.core.services.data.models.UpdatedItem;
import com.exadel.etoolbox.linkinspector.core.services.data.models.DataFilter;
import com.exadel.etoolbox.linkinspector.core.services.data.models.GridResource;
import com.exadel.etoolbox.linkinspector.core.services.exceptions.DataFeedException;
import org.apache.sling.api.resource.Resource;

import java.util.List;

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

    /**
     * Collects broken links and generates json data feed for further usage in the Link Inspector grid.
     */
    void generateDataFeed();

    /**
     * Parses the data feed to the list of resources({@link Resource}) for further adapting them to view models
     * and displaying in the Link Inspector grid. The number of output items is limited for the sake of UX consistency.
     *
     * @return the list of resources({@link Resource}) based on the data feed
     */
    List<Resource> dataFeedToResources(DataFilter filter);

    /**
     * Parses the data feed to the list of models({@link GridResource}). The number of output items is not limited.
     *
     * @return the list of view items({@link GridResource}) based on the data feed
     */
    List<GridResource> dataFeedToGridResources();


    /**
     * Method for modification links value in the data feed
     *
     * @param updatedItems - {@code List<UpdatedItem>} containing the updated items information
     * @param updateGridResource - {@code boolean} flag indicating whether the grid resource should be updated
     */
    void modifyDataFeed(List<UpdatedItem> updatedItems, boolean updateGridResource);

    /**
     * Method for deleting data in data feed
     */
    void deleteDataFeed() throws DataFeedException;
}