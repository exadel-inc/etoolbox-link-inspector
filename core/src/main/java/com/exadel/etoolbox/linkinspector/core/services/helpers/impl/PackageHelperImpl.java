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

import com.exadel.etoolbox.linkinspector.core.services.helpers.PackageHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.apache.jackrabbit.vault.fs.filter.DefaultPathFilter;
import org.apache.jackrabbit.vault.packaging.*;
import org.apache.jackrabbit.vault.util.DefaultProgressListener;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

/**
 * <p><u>Note</u>: This class is not a part of the public API and is subject to change. Do not use it in your own code</p>
 * Implements {@link PackageHelper} interface to provide an OSGi service which handles JCR packages creation.
 */
@Slf4j
@Component(service = PackageHelper.class)
public class PackageHelperImpl implements PackageHelper {

    @Reference
    private Packaging packaging;

    /**
     * The path filter for child nodes exclusion
     */
    private static final String EXCLUDE_CHILDREN_PATTERN = "/.*";

    /**
     * {@inheritDoc}
     */
    @Override
    public JcrPackage createPackageForPaths(Collection<String> paths, Session session,
                                            String groupName, String name, String version,
                                            boolean excludeChildren, boolean assemble)
            throws IOException, RepositoryException, PackageException {
        final JcrPackageManager jcrPackageManager = packaging.getPackageManager(session);
        try (JcrPackage jcrPackage = jcrPackageManager.create(groupName, name, version)) {
            final DefaultWorkspaceFilter workspaceFilter = new DefaultWorkspaceFilter();
            paths.stream()
                    .map(path -> pathToFilterSet(path, excludeChildren))
                    .forEach(workspaceFilter::add);
            Optional<JcrPackageDefinition> packageDefinition = Optional.ofNullable(jcrPackage.getDefinition());
            if (packageDefinition.isPresent()) {
                packageDefinition.get().setFilter(workspaceFilter, true);
                session.save();
                if (assemble) {
                    jcrPackageManager.assemble(jcrPackage, new DefaultProgressListener());
                }
                return jcrPackage;
            }
        }
        return null;
    }

    private PathFilterSet pathToFilterSet(String path, boolean excludeChildren) {
        PathFilterSet pathFilterSet = new PathFilterSet(path);
        if (excludeChildren) {
            pathFilterSet.addExclude(new DefaultPathFilter(path + EXCLUDE_CHILDREN_PATTERN));
        }
        return pathFilterSet;
    }
}