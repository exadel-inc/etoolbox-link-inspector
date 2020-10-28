package com.exadel.linkchecker.core.services.data;

import com.exadel.linkchecker.core.services.data.models.GridResource;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.Set;

public interface GridResourcesGenerator {
    Set<GridResource> generateGridResources(String gridResourceType, ResourceResolver resourceResolver);
}