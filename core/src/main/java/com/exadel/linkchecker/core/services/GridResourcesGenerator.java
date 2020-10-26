package com.exadel.linkchecker.core.services;

import org.apache.sling.api.resource.Resource;

import java.util.List;

public interface GridResourcesGenerator {
    List<Resource> generateGridResources(String gridResourceType);
}