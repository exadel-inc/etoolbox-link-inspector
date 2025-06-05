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

package com.exadel.etoolbox.linkinspector.core.servlets;

import com.exadel.etoolbox.linkinspector.core.services.job.DataFeedJobExecutor;
import com.exadel.etoolbox.linkinspector.core.services.job.SlingJobUtil;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import junitx.util.PrivateAccessor;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.event.jobs.JobManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(AemContextExtension.class)
class GenerateDataFeedServletTest {
    private static final String JOB_MANAGER_FIELD = "jobManager";

    private final GenerateDataFeedServlet fixture = new GenerateDataFeedServlet();

    SlingHttpServletRequest requestMock;
    SlingHttpServletResponse responseMock;
    ResourceResolver resourceResolverMock;
    JobManager jobManager;

    @BeforeEach
    void setup() throws NoSuchFieldException {
        requestMock = mock(SlingHttpServletRequest.class);
        responseMock = mock(SlingHttpServletResponse.class);
        resourceResolverMock = mock(ResourceResolver.class);

        jobManager = mock(JobManager.class);
        PrivateAccessor.setField(fixture, JOB_MANAGER_FIELD, jobManager);

        when(requestMock.getResourceResolver()).thenReturn(resourceResolverMock);
    }

    @Test
    void testDoGet() {
        verifySlingJobAdded();
    }

    private void verifySlingJobAdded() {
        try (MockedStatic<SlingJobUtil> slingJobUtil = mockStatic(SlingJobUtil.class)) {
            fixture.doGet(requestMock, responseMock);

            slingJobUtil.verify(() ->
                    SlingJobUtil.addJob(any(JobManager.class), eq(DataFeedJobExecutor.GENERATE_DATA_FEED_TOPIC), anyMap())
            );
        }
    }
}