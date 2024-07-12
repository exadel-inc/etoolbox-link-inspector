package com.exadel.etoolbox.linkinspector.core.services.mocks;

import com.exadel.etoolbox.linkinspector.api.Link;
import com.exadel.etoolbox.linkinspector.api.LinkResolver;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.Collection;
import java.util.Collections;

public class MockCustomLinkResolver implements LinkResolver {
    @Override
    public String getId() {
        return "custom";
    }

    @Override
    public Collection<Link> getLinks(String source) {
        return Collections.emptyList();
    }

    @Override
    public void validate(Link link, ResourceResolver resourceResolver) {
        // No operation
    }
}
