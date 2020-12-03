package com.exadel.linkchecker.core.services.data;

import com.exadel.linkchecker.core.services.data.models.GridResource;
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