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

    /**
     * Gets a service ResourceResolver for system operations.
     * <p>
     * This ResourceResolver is obtained using service user credentials and should be used
     * for administrative operations that require elevated privileges. The caller is not
     * responsible for closing this ResourceResolver.
     *
     * @return A ResourceResolver with service privileges
     */
    ResourceResolver getServiceResourceResolver();

    /**
     * Returns the {@link ResourceResolver} for the current thread
     *
     * @see org.apache.sling.api.resource.ResourceResolverFactory#getThreadResourceResolver()
     */
    ResourceResolver getThreadResourceResolver();

    /**
     * Checks if a session has the specified permissions at the given path.
     * <p>
     * This method verifies whether the provided session has the requested permissions
     * (like read, modify, add, remove) at the specified repository path.
     *
     * @param session The JCR session to check permissions for
     * @param path The repository path to check permissions at
     * @param permissions A comma-separated list of permission names to check
     * @return True if the session has all specified permissions, false otherwise
     */
    boolean hasPermissions(Session session, String path, String permissions);

    /**
     * Checks if a session has both read and write permissions at the given path.
     * <p>
     * This is a convenience method that combines read and write permission checks into one call.
     * It's particularly useful for operations that need to both read and modify content.
     *
     * @param session The JCR session to check permissions for
     * @param path The repository path to check permissions at
     * @return True if the session has both read and write permissions, false otherwise
     */
    boolean hasReadWritePermissions(Session session, String path);

    /**
     * Creates a resource at the specified path if it doesn't already exist.
     * <p>
     * This method ensures a resource exists at the given path, creating it and any
     * necessary parent resources if they don't exist. The created resource will have
     * the specified resource type, while intermediate resources will use the intermediate
     * resource type.
     *
     * @param path The repository path where the resource should exist
     * @param resourceType The resource type to assign to the created resource
     * @param intermediateResourceType The resource type to assign to any intermediate resources that need to be created
     */
    void createResourceIfNotExist(String path, String resourceType, String intermediateResourceType);
}