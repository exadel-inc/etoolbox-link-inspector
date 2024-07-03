package com.exadel.etoolbox.linkinspector.core.services.mocks;

import com.exadel.etoolbox.linkinspector.core.services.helpers.RepositoryHelper;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.Session;

public class MockRepositoryHelper implements RepositoryHelper {

    private final ResourceResolver resourceResolver;
    private int resourceCounter = 0;

    public MockRepositoryHelper(ResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    @Override
    public ResourceResolver getServiceResourceResolver() {
        return resourceResolver;
    }

    @Override
    public ResourceResolver getThreadResourceResolver() {
        return resourceResolver;
    }

    @Override
    public boolean hasPermissions(Session session, String path, String permissions) {
        return true;
    }

    @Override
    public boolean hasReadWritePermissions(Session session, String path) {
        return true;
    }

    @Override
    public void createResourceIfNotExist(String path, String resourceType, String intermediateResourceType) {
        resourceCounter++;
    }

    public int getCreationsCount() {
        return resourceCounter;
    }
}
