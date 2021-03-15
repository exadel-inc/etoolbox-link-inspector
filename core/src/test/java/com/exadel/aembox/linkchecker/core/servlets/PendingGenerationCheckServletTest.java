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

import com.exadel.aembox.linkchecker.core.services.data.DataFeedService;
import com.exadel.aembox.linkchecker.core.services.helpers.RepositoryHelper;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import junitx.util.PrivateAccessor;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(AemContextExtension.class)
class PendingGenerationCheckServletTest {
    private static final String REPOSITORY_HELPER_FIELD = "repositoryHelper";

    private final AemContext context = new AemContext();

    private final PendingGenerationCheckServlet fixture = new PendingGenerationCheckServlet();

    private MockSlingHttpServletRequest request;
    private MockSlingHttpServletResponse response;

    @BeforeEach
    void setup() throws NoSuchFieldException {
        RepositoryHelper repositoryHelper = mock(RepositoryHelper.class);
        PrivateAccessor.setField(fixture, REPOSITORY_HELPER_FIELD, repositoryHelper);

        when(repositoryHelper.getServiceResourceResolver()).thenReturn(context.resourceResolver());

        request = context.request();
        response = context.response();
    }

    @Test
    void testPendingNodePresent() {
        context.create().resource(DataFeedService.PENDING_GENERATION_NODE);

        fixture.doPost(request, response);

        assertEquals(HttpStatus.SC_OK, response.getStatus());
    }

    @Test
    void testPendingNodeAbsent() {
        fixture.doPost(request, response);

        assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatus());
    }
}