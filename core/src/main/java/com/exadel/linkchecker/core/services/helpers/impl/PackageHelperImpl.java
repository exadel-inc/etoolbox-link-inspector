package com.exadel.linkchecker.core.services.helpers.impl;

import com.exadel.linkchecker.core.services.helpers.PackageHelper;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.apache.jackrabbit.vault.fs.filter.DefaultPathFilter;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.jackrabbit.vault.util.DefaultProgressListener;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

@Component(service = PackageHelper.class)
public class PackageHelperImpl implements PackageHelper {

    @Reference
    private Packaging packaging;

    private static final String EXCLUDE_CHILDREN_PATTERN = "/.*";

    public JcrPackage createPackageForPaths(Collection<String> paths, Session session,
                                            String groupName, String name, String version,
                                            boolean excludeChildren, boolean assemble)
            throws IOException, RepositoryException, PackageException {
        final JcrPackageManager jcrPackageManager = packaging.getPackageManager(session);
        try(final JcrPackage jcrPackage = jcrPackageManager.create(groupName, name, version)) {
            final DefaultWorkspaceFilter workspaceFilter = new DefaultWorkspaceFilter();
            paths.stream()
                    .map(path -> pathToFilterSet(path, excludeChildren))
                    .forEach(workspaceFilter::add);
            Optional.ofNullable(jcrPackage.getDefinition())
                    .ifPresent(jcrPackageDefinition -> jcrPackageDefinition.setFilter(workspaceFilter, true));
            session.save();
            if (assemble) {
                jcrPackageManager.assemble(jcrPackage, new DefaultProgressListener());
            }
            return jcrPackage;
        }
    }

    private PathFilterSet pathToFilterSet(String path, boolean excludeChildren) {
        PathFilterSet pathFilterSet = new PathFilterSet(path);
        if (excludeChildren) {
            pathFilterSet.addExclude(new DefaultPathFilter(path + EXCLUDE_CHILDREN_PATTERN));
        }
        return pathFilterSet;
    }
}