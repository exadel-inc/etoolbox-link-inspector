package com.exadel.etoolbox.linkinspector.core.services.cache;

import com.exadel.etoolbox.linkinspector.core.services.data.models.GridResource;

import java.util.concurrent.CopyOnWriteArrayList;

public interface LinkCache {

    CopyOnWriteArrayList<GridResource> getGridResourcesList();
}
