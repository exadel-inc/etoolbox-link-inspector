package com.exadel.etoolbox.linkinspector.core.services.cache;

import com.exadel.etoolbox.linkinspector.core.services.data.models.GridResource;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public interface GridResourcesCache {

    CopyOnWriteArrayList<GridResource> getGridResourcesList();

    void setGridResourcesList(List<GridResource> gridResources);

    void clearCache();
}
