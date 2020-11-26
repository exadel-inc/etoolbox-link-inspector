package com.exadel.linkchecker.core.services;

import org.apache.sling.api.resource.ResourceResolver;
import javax.jcr.Session;

public interface RepositoryHelper {
    ResourceResolver getServiceResourceResolver();

    ResourceResolver getThreadResourceResolver();

    boolean hasPermissions(Session session, String path, String permissions);

    boolean hasReadWritePermissions(Session session, String path);

    void createResourceIfNotExist(String path, String resourceType, String intermediateResourceType);
}