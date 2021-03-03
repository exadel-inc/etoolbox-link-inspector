package com.exadel.aembox.linkchecker.core.services.helpers;

import org.apache.sling.api.resource.ResourceResolver;
import javax.jcr.Session;

public interface RepositoryHelper {
    ResourceResolver getServiceResourceResolver();

    /**
     * @see org.apache.sling.api.resource.ResourceResolverFactory#getThreadResourceResolver()
     */
    ResourceResolver getThreadResourceResolver();

    boolean hasPermissions(Session session, String path, String permissions);

    boolean hasReadWritePermissions(Session session, String path);

    void createResourceIfNotExist(String path, String resourceType, String intermediateResourceType);
}