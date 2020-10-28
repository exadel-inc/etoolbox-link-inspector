package com.exadel.linkchecker.core.services.impl;

import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.exadel.linkchecker.core.services.data.DataFeedService;
import com.exadel.linkchecker.core.services.GridDataSource;
import com.adobe.granite.ui.components.ds.DataSource;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

@Component(service = GridDataSource.class)
public class GridDataSourceImpl implements GridDataSource {
    private static final Logger LOG = LoggerFactory.getLogger(GridDataSource.class);

    @Reference
    private DataFeedService dataFeedService;

    @Override
    public DataSource getDataSource(HttpServletRequest request, Object cmp, Resource resource) {
        LOG.debug("GridDataSource initialization");
        return new SimpleDataSource(dataFeedService.dataFeedToResources().iterator());
    }
}
