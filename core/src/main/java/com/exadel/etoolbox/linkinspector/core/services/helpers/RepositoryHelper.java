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

package com.exadel.etoolbox.linkinspector.core.services.helpers;

import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.Session;

/**
 * Provides methods for assisting with the repository related operations.
 */
public interface RepositoryHelper {
    ResourceResolver getServiceResourceResolver();

    /**
     * Returns the {@link ResourceResolver} for the current thread
     *
     * @see org.apache.sling.api.resource.ResourceResolverFactory#getThreadResourceResolver()
     */
    ResourceResolver getThreadResourceResolver();

    boolean hasPermissions(Session session, String path, String permissions);

    boolean hasReadWritePermissions(Session session, String path);

    void createResourceIfNotExist(String path, String resourceType, String intermediateResourceType);
}