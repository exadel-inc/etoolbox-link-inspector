package com.exadel.linkchecker.core.services.impl;

import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.exadel.linkchecker.core.services.GridResourcesGenerator;
import com.exadel.linkchecker.core.services.GridDataSource;
import com.adobe.granite.ui.components.ds.DataSource;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

@Component(service = GridDataSource.class)
public class GridDataSourceImpl implements GridDataSource {
    private static final Logger LOG = LoggerFactory.getLogger(GridDataSource.class);

    private static final String PN_DATASOURCE = "datasource";
    private static final String PN_ITEM_RESOURCE_TYPE = "itemResourceType";

    @Reference
    private GridResourcesGenerator gridResourcesGenerator;

    @Override
    public DataSource getDataSource(HttpServletRequest request, Object cmp, Resource resource) {
        String gridResourceType = getDataSourceResourceType(resource);
        LOG.trace("Grid items resource type: {}", gridResourceType);
        return new SimpleDataSource(gridResourcesGenerator.generateGridResources(gridResourceType).iterator());
    }

    private String getDataSourceResourceType(Resource resource) {
        return Optional.ofNullable(resource.getChild(PN_DATASOURCE))
                .map(ResourceUtil::getValueMap)
                .map(valueMap -> valueMap.get(PN_ITEM_RESOURCE_TYPE, String.class))
                .orElse(StringUtils.EMPTY);
    }
}
