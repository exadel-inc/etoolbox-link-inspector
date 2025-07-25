/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exadel.etoolbox.linkinspector.core.services.helpers.impl;

import com.exadel.etoolbox.linkinspector.core.services.helpers.RepositoryHelper;
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
import java.util.Collections;

/**
 * Implements {@link RepositoryHelper} interface to provide an OSGi service which handles repository related operations.
 * <p><u>Note</u>: This class is not a part of the public API and is subject to change. Do not use it in your own code</p>
 */
@Component(service = RepositoryHelper.class)
public class RepositoryHelperImpl implements RepositoryHelper {
    private static final Logger LOG = LoggerFactory.getLogger(RepositoryHelperImpl.class);

    private static final String LINK_INSPECTOR_SERVICE_NAME = "etoolbox-link-inspector-service";

    private static final String READ_WRITE_PERMISSIONS = String.join(",", Session.ACTION_READ, Session.ACTION_SET_PROPERTY);

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceResolver getServiceResourceResolver() {
        try {
            return resourceResolverFactory.getServiceResourceResolver(
                    Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, LINK_INSPECTOR_SERVICE_NAME));
        } catch (LoginException e) {
            LOG.error("Failed to get service resource resolver", e);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceResolver getThreadResourceResolver() {
        return resourceResolverFactory.getThreadResourceResolver();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasPermissions(Session session, String path, String permissions) {
        try {
            return session.hasPermission(path, permissions);
        } catch (RepositoryException e) {
            LOG.error("Failed to check permissions '{}' for resource {}: {}", permissions, path, e.getMessage());
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasReadWritePermissions(Session session, String path) {
        return hasPermissions(session, path, READ_WRITE_PERMISSIONS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
            LOG.error(String.format("Failed to create the resource %s", path), e);
        }
    }
}