package com.exadel.linkchecker.core.services.impl;

import com.exadel.linkchecker.core.services.RepositoryHelper;
import com.google.common.collect.ImmutableMap;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

@Component(service = RepositoryHelper.class)
public class RepositoryHelperImpl implements RepositoryHelper {
    private static final Logger LOG = LoggerFactory.getLogger(RepositoryHelper.class);

    private static final String LINK_CHECKER_SERVICE_NAME = "exadel-linkchecker-service";

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    public ResourceResolver getServiceResourceResolver() {
        try {
            return resourceResolverFactory.getServiceResourceResolver(
                    ImmutableMap.of(ResourceResolverFactory.SUBSERVICE, LINK_CHECKER_SERVICE_NAME));
        } catch (LoginException e) {
            LOG.error("Failed to get service resource resolver", e);
        }
        return null;
    }

    public ResourceResolver getThreadResourceResolver() {
        return resourceResolverFactory.getThreadResourceResolver();
    }

    public boolean hasPermissions(Session session, String path, String permissions) {
        try {
            return session.hasPermission(path, permissions);
        } catch (RepositoryException e) {
            LOG.error("Failed to check permissions '{}' for resource {}: {}", permissions, path, e.getMessage());
        }
        return false;
    }

    public void createResourceIfNotExist(String path, String resourceType, String intermediateResourceType) {
        try (ResourceResolver serviceResourceResolver = getServiceResourceResolver()) {
            ResourceUtil.getOrCreateResource(
                    serviceResourceResolver,
                    path,
                    resourceType,
                    intermediateResourceType,
                    true
            );
        } catch (PersistenceException e) {
            LOG.error(String.format("Failed to create the node %s", path), e);
        }
    }
}