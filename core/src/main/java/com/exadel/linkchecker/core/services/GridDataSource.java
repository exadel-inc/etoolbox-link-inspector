package com.exadel.linkchecker.core.services;

import com.adobe.granite.ui.components.ds.DataSource;
import org.apache.sling.api.resource.Resource;

import javax.servlet.http.HttpServletRequest;

public interface GridDataSource {
    DataSource getDataSource(HttpServletRequest request, Object cmp, Resource resource);
}
