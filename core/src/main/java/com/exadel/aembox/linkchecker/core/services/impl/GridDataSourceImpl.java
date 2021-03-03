package com.exadel.aembox.linkchecker.core.services.impl;

import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.exadel.aembox.linkchecker.core.services.GridDataSource;
import com.exadel.aembox.linkchecker.core.services.data.DataFeedService;
import com.adobe.granite.ui.components.ds.DataSource;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = GridDataSource.class)
public class GridDataSourceImpl implements GridDataSource {
    private static final Logger LOG = LoggerFactory.getLogger(GridDataSource.class);

    @Reference
    private DataFeedService dataFeedService;

    @Override
    public DataSource getDataSource() {
        LOG.debug("GridDataSource initialization");
        return new SimpleDataSource(dataFeedService.dataFeedToResources().iterator());
    }
}
