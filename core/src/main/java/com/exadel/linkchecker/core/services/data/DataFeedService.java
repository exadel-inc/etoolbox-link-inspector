package com.exadel.linkchecker.core.services.data;

import com.exadel.linkchecker.core.models.GridViewItem;
import org.apache.sling.api.resource.Resource;

import java.util.List;

public interface DataFeedService {
    /**
     * Collects broken links and generates json data feed for further usage in the Link Checker grid.
     */
    void generateDataFeed();

    /**
     * Parses the data feed to the list of resources({@link Resource}) for further adapting to view models
     * and displaying them in the Link Checker grid.
     *
     * @return the list of resources({@link Resource}) based on the data feed
     */
    List<Resource> dataFeedToResources();

    /**
     * Parses the data feed to the list of view items({@link GridViewItem}).
     *
     * @return the list of view items({@link GridViewItem}) based on the data feed
     */
    List<GridViewItem> dataFeedToViewItems();
}