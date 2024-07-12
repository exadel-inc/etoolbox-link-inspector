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

import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import junitx.util.PrivateAccessor;
import org.apache.jackrabbit.vault.fs.api.ProgressTrackerListener;
import org.apache.jackrabbit.vault.packaging.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(AemContextExtension.class)
class PackageHelperImplTest {
    private static final String PACKAGING_FIELD = "packaging";

    private static final List<String> TEST_PATHS = Arrays.asList("/content/path1", "/content/path2");
    private static final String TEST_GROUP_NAME = "test group name";
    private static final String TEST_PACKAGE_NAME = "test-name";
    private static final String TEST_VERSION = "1.0";

    private final PackageHelperImpl packageHelper = new PackageHelperImpl();

    private JcrPackageManager jcrPackageManager;
    private JcrPackage jcrPackage;
    private Session session;

    @BeforeEach
    void setup() throws NoSuchFieldException, IOException, RepositoryException {
        Packaging packaging = mock(Packaging.class);
        jcrPackageManager = mock(JcrPackageManager.class);
        jcrPackage = mock(JcrPackage.class);
        session = mock(Session.class);
        PrivateAccessor.setField(packageHelper, PACKAGING_FIELD, packaging);

        when(packaging.getPackageManager(session)).thenReturn(jcrPackageManager);
        when(jcrPackageManager.create(TEST_GROUP_NAME, TEST_PACKAGE_NAME, TEST_VERSION)).thenReturn(jcrPackage);
    }

    @Test
    void testCreatePackageForPaths_assemble() throws RepositoryException, PackageException, IOException {
        when(jcrPackage.getDefinition()).thenReturn(mock(JcrPackageDefinition.class));

        JcrPackage resultPackage = packageHelper.createPackageForPaths(TEST_PATHS, session, TEST_GROUP_NAME, TEST_PACKAGE_NAME, TEST_VERSION, true, true);

        verify(jcrPackageManager, atLeastOnce()).assemble(eq(jcrPackage), any(ProgressTrackerListener.class));
        assertNotNull(resultPackage);
    }

    @Test
    void testCreatePackageForPaths_nullDefinition() throws RepositoryException, PackageException, IOException {
        when(jcrPackage.getDefinition()).thenReturn(null);

        JcrPackage resultPackage = packageHelper.createPackageForPaths(TEST_PATHS, session, TEST_GROUP_NAME, TEST_PACKAGE_NAME, TEST_VERSION, true, true);

        verify(jcrPackageManager, never()).assemble(eq(jcrPackage), any(ProgressTrackerListener.class));
        assertNull(resultPackage);
    }
}