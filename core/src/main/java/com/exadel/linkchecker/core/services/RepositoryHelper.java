package com.exadel.linkchecker.core.services;

import org.apache.sling.api.resource.ResourceResolver;
import javax.jcr.Session;

public interface RepositoryHelper {
    ResourceResolver getServiceResourceResolver();

    ResourceResolver getThreadResourceResolver();

    boolean hasPermissions(Session session, String path, String permissions);

    void createResourceIfNotExist(String path, String resourceType, String intermediateResourceType);
}