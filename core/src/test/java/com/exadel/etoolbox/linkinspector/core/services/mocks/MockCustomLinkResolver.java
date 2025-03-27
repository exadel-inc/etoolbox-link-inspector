package com.exadel.etoolbox.linkinspector.core.services.mocks;

import com.exadel.etoolbox.linkinspector.api.Result;
import com.exadel.etoolbox.linkinspector.api.Resolver;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.Collection;
import java.util.Collections;

public class MockCustomLinkResolver implements Resolver {
    @Override
    public String getId() {
        return "custom";
    }

    @Override
    public Collection<Result> getResults(String source) {
        return Collections.emptyList();
    }

    @Override
    public void validate(Result result, ResourceResolver resourceResolver) {
        // No operation
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
