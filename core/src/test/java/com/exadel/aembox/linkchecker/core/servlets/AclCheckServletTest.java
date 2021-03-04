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

package com.exadel.aembox.linkchecker.core.servlets;

import com.exadel.aembox.linkchecker.core.services.helpers.RepositoryHelper;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import junitx.util.PrivateAccessor;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import javax.jcr.Session;
import javax.json.Json;

@ExtendWith(AemContextExtension.class)
class AclCheckServletTest {
    private static final String REPOSITORY_HELPER_FIELD = "repositoryHelper";

    private static final String PATH_REQUEST_PARAM = "path";
    private static final String PERMISSIONS_REQUEST_PARAM = "permissions";
    private static final String HAS_PERMISSIONS_RESPONSE_PARAM = "hasPermissions";

    private static final String TEST_PATH = "/content/test";
    private static final String TEST_PERMISSIONS = "testPermissions";

    private final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    private final AclCheckServlet fixture = new AclCheckServlet();

    private RepositoryHelper repositoryHelper;

    private MockSlingHttpServletRequest request;
    private MockSlingHttpServletResponse response;

    @BeforeEach
    void setup() throws NoSuchFieldException {
        repositoryHelper = mock(RepositoryHelper.class);
        PrivateAccessor.setField(fixture, REPOSITORY_HELPER_FIELD, repositoryHelper);
        request = context.request();
        response = context.response();
    }

    @Test
    void testHasPermissions() {
        request.addRequestParameter(PATH_REQUEST_PARAM, TEST_PATH);
        request.addRequestParameter(PERMISSIONS_REQUEST_PARAM, TEST_PERMISSIONS);

        when(repositoryHelper.hasPermissions(any(Session.class), eq(TEST_PATH), eq(TEST_PERMISSIONS)))
                .thenReturn(true);

        fixture.doPost(request, response);
        String expectedJsonResponse = Json.createObjectBuilder()
                .add(HAS_PERMISSIONS_RESPONSE_PARAM, true)
                .build()
                .toString();
        String jsonResponse = response.getOutputAsString();

        assertEquals(expectedJsonResponse, jsonResponse);
    }

    @Test
    void testEmptyParams() {
        fixture.doPost(request, response);

        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        verifyNoInteractions(repositoryHelper);
    }

    @Test
    void testEmptySession() {
        SlingHttpServletRequest requestMock = mock(SlingHttpServletRequest.class);
        SlingHttpServletResponse responseMock = mock(SlingHttpServletResponse.class);
        ResourceResolver resourceResolverMock = mock(ResourceResolver.class);
        mockRequestParam(PATH_REQUEST_PARAM, TEST_PATH, requestMock);
        mockRequestParam(PERMISSIONS_REQUEST_PARAM, TEST_PERMISSIONS, requestMock);

        when(requestMock.getResourceResolver()).thenReturn(resourceResolverMock);
        when(resourceResolverMock.adaptTo(Session.class)).thenReturn(null);

        fixture.doPost(requestMock, responseMock);

        verify(responseMock).setStatus(HttpStatus.SC_BAD_REQUEST);
        verifyNoInteractions(repositoryHelper);
    }

    private void mockRequestParam(String paramName, String value, SlingHttpServletRequest requestMock) {
        RequestParameter requestParameter = mock(RequestParameter.class);
        when(requestMock.getRequestParameter(paramName)).thenReturn(requestParameter);
        when(requestParameter.getString()).thenReturn(value);
    }
}