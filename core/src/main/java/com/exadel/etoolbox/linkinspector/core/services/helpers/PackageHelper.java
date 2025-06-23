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

import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.PackageException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.util.Collection;

/**
 * <p><u>Note</u>: This class is not a part of the public API and is subject to change. Do not use it in your own code</p>
 * Handles JCR packages creation
 */
public interface PackageHelper {
    /**
     * Creates a JCR package based on the specified parameters.
     *
     * @param paths           - the collection of path for adding to the package
     * @param session         - a JCR session associated to the user who initialized package creation
     * @param groupName       - group of the new package
     * @param name            - name of the new package
     * @param version         - version of the new package; can be null
     * @param excludeChildren - indicates if child nodes should be exclude from the package paths
     * @param assemble        - indicates if the package should be assembled after creation
     * @return a new JCR package
     * @throws IOException         - if an I/O exception occurs
     * @throws RepositoryException - if a repository error occurs
     * @throws PackageException    - if an internal error occurs
     */
    JcrPackage createPackageForPaths(Collection<String> paths, Session session,
                                     String groupName, String name, String version,
                                     boolean excludeChildren, boolean assemble)
            throws IOException, RepositoryException, PackageException;
}