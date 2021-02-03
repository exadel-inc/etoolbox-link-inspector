package com.exadel.linkchecker.core.services.data;

import com.exadel.linkchecker.core.services.data.models.GridResource;
import org.apache.sling.api.resource.Resource;

import java.util.List;

public interface DataFeedService {
    /**
     * If the node presents, users are informed that data feed regeneration is required
     * in order to display up-to-date results.
     */
    String PENDING_GENERATION_NODE = "/content/exadel-linkchecker/data/pendingDataFeedUpdate";

    /**
     * Collects broken links and generates json data feed for further usage in the Link Checker grid.
     */
    void generateDataFeed();

    /**
     * Parses the data feed to the list of resources({@link Resource}) for further adapting to view models
     * and displaying them in the Link Checker grid. The number of output items is limited.
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