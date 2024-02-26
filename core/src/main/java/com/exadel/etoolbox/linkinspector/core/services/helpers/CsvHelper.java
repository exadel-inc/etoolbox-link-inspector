package com.exadel.etoolbox.linkinspector.core.services.helpers;

import com.exadel.etoolbox.linkinspector.core.models.ui.GridViewItem;
import com.exadel.etoolbox.linkinspector.core.services.data.models.GridResource;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.List;

public interface CsvHelper {

    /**
     * Reading items from csv and adapt them to the {@link GridResource}
     *
     * @param resourceResolver - {@link ResourceResolver}
     * @param page             - page number of csv report
     * @return the {@link List<GridResource>} representing the List of extracted grid resources form csv
     */
    List<GridResource> readCsvReport(ResourceResolver resourceResolver, int page);

    /**
     * Creating csv reports by list of {@link GridViewItem}. Reports are splitting on parts and used like pages.
     *
     * @param resourceResolver - {@link ResourceResolver}
     * @param gridViewItems    - {@link List<GridViewItem>}
     */
    void generateCsvReport(ResourceResolver resourceResolver, List<GridViewItem> gridViewItems);

    /**
     * Method for the csv report modification with updated link values.
     *
     * @param resourceResolver - {@link ResourceResolver}
     * @param gridViewItems    - {@link List<GridViewItem>}
     * @param page             - page number of csv report
     */
    void modifyCsvReport(ResourceResolver resourceResolver, List<GridViewItem> gridViewItems, int page);
}
