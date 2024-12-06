package com.exadel.etoolbox.linkinspector.core.services.mocks;

import com.exadel.etoolbox.linkinspector.core.services.helpers.RepositoryHelper;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.wrappers.ResourceResolverWrapper;

import javax.jcr.Session;

public class MockRepositoryHelper implements RepositoryHelper {

    private final ResourceResolver basicResolver;
    private int resourceCounter = 0;

    public MockRepositoryHelper(ResourceResolver basicResolver) {
        this.basicResolver = basicResolver;
    }

    @Override
    public ResourceResolver getServiceResourceResolver() {
        return new OneTimeResourceResolver(basicResolver);
    }

    @Override
    public ResourceResolver getThreadResourceResolver() {
        return new OneTimeResourceResolver(basicResolver);
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

    private static class OneTimeResourceResolver extends ResourceResolverWrapper {
        OneTimeResourceResolver(ResourceResolver resourceResolver) {
            super(resourceResolver);
        }

        @Override
        public void close() {
            // No operation
        }
    }
}
