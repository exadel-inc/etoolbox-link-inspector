package com.exadel.linkchecker.core.services.data;

import org.apache.sling.api.resource.Resource;

import java.util.List;

public interface DataFeedService {
    void generateDataFeed();

    List<Resource> dataFeedToResources();
}