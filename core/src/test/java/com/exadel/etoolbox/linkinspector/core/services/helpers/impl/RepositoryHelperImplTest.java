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

import com.day.crx.JcrConstants;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import junitx.util.PrivateAccessor;
import org.apache.sling.api.resource.*;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(AemContextExtension.class)
class RepositoryHelperImplTest {
    private static final String RESOURCE_RESOLVER_FACTORY_FIELD = "resourceResolverFactory";

    private static final String TEST_RESOURCE_PATH = "/content/test-resource";
    private static final String TEST_PERMISSIONS = "testPermissions";

    private final AemContext context = new AemContext();

    private final RepositoryHelperImpl repositoryHelper = new RepositoryHelperImpl();

    private ResourceResolverFactory resourceResolverFactory;

    @BeforeEach
    void setup() throws NoSuchFieldException {
        resourceResolverFactory = context.getService(ResourceResolverFactory.class);
        PrivateAccessor.setField(repositoryHelper, RESOURCE_RESOLVER_FACTORY_FIELD, resourceResolverFactory);
    }

    @Test
    void testGetServiceResourceResolver() {
        assertNotNull(repositoryHelper.getServiceResourceResolver());
    }

    @Test
    void testGetServiceResourceResolver_loginException() throws NoSuchFieldException, LoginException {
        ResourceResolverFactory resourceResolverFactoryMock = mock(ResourceResolverFactory.class);
        PrivateAccessor.setField(repositoryHelper, RESOURCE_RESOLVER_FACTORY_FIELD, resourceResolverFactoryMock);

        when(resourceResolverFactoryMock.getServiceResourceResolver(anyMap())).thenThrow(new LoginException());

        assertNull(repositoryHelper.getServiceResourceResolver());
    }

    @Test
    void testGetThreadResourceResolver() {
        assertEquals(resourceResolverFactory.getThreadResourceResolver(), repositoryHelper.getThreadResourceResolver());
    }

    @Test
    void testCreateResourceIfNotExist() {
        try (MockedStatic<ResourceUtil> resourceUtil = mockStatic(ResourceUtil.class)) {
            resourceUtil.when(() ->
                    ResourceUtil.getOrCreateResource(any(ResourceResolver.class), eq(TEST_RESOURCE_PATH),
                            eq(JcrConstants.NT_UNSTRUCTURED), eq(JcrResourceConstants.NT_SLING_FOLDER), eq(true))
            ).thenThrow(new PersistenceException());
            repositoryHelper.createResourceIfNotExist(TEST_RESOURCE_PATH, JcrConstants.NT_UNSTRUCTURED, JcrResourceConstants.NT_SLING_FOLDER);
        }

        assertNull(repositoryHelper.getServiceResourceResolver().getResource(TEST_RESOURCE_PATH));
    }

    @Test
    void testHasReadWritePermissions() throws RepositoryException {
        Session session = mock(Session.class);
        when(session.hasPermission(eq(TEST_RESOURCE_PATH), anyString())).thenReturn(true);

        assertTrue(repositoryHelper.hasReadWritePermissions(session, TEST_RESOURCE_PATH));
    }

    @Test
    void testHasPermissions() throws RepositoryException {
        Session session = mock(Session.class);
        when(session.hasPermission(eq(TEST_RESOURCE_PATH), eq(TEST_PERMISSIONS))).thenReturn(true);

        assertTrue(repositoryHelper.hasPermissions(session, TEST_RESOURCE_PATH, TEST_PERMISSIONS));
    }

    @Test
    void testHasPermissions_exception() throws RepositoryException {
        Session session = mock(Session.class);
        when(session.hasPermission(eq(TEST_RESOURCE_PATH), eq(TEST_PERMISSIONS))).thenThrow(new RepositoryException());

        assertFalse(repositoryHelper.hasPermissions(session, TEST_RESOURCE_PATH, TEST_PERMISSIONS));
    }
}