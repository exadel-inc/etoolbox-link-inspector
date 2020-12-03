package com.exadel.linkchecker.core.services.helpers;

import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.PackageException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.util.Collection;

public interface PackageHelper {
    JcrPackage createPackageForPaths(Collection<String> paths, Session session,
                                     String groupName, String name, String version,
                                     boolean excludeChildren, boolean assemble)
            throws IOException, RepositoryException, PackageException;
}