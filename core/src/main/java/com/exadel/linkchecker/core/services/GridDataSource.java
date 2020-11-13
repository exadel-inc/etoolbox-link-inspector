package com.exadel.linkchecker.core.services;

import com.adobe.granite.ui.components.ds.DataSource;

public interface GridDataSource {
    /**
     * Generates {@link DataSource} necessary for displaying items on the Link Checker's page
     * withing the grid (granite/ui/components/coral/foundation/table)
     *
     * @return
     */
    DataSource getDataSource();
}
