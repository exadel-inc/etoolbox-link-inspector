package com.exadel.etoolbox.linkinspector.core.services.mocks;

import com.exadel.etoolbox.linkinspector.core.services.data.models.UpdatedItem;
import com.exadel.etoolbox.linkinspector.core.services.data.DataFeedService;
import com.exadel.etoolbox.linkinspector.core.services.data.models.DataFilter;
import com.exadel.etoolbox.linkinspector.core.services.data.models.GridResource;
import org.apache.sling.api.resource.Resource;

import java.util.Collections;
import java.util.List;

public class MockDataFeedService implements DataFeedService {
    @Override
    public void generateDataFeed() {
        // No operation
    }

    @Override
    public List<Resource> dataFeedToResources(DataFilter filter) {
        return Collections.emptyList();
    }

    @Override
    public List<GridResource> dataFeedToGridResources() {
        return Collections.emptyList();
    }

    @Override
    public void modifyDataFeed(List<UpdatedItem> updatedItems) {
        // No operation
    }

    @Override
    public void deleteDataFeed() {
        // No operation
    }
}
