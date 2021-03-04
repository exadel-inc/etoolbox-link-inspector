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

package com.exadel.aembox.linkchecker.core.servlets.data;

import com.exadel.aembox.linkchecker.core.services.job.DataFeedJobExecutor;
import com.exadel.aembox.linkchecker.core.services.job.SlingJobUtil;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import junitx.util.PrivateAccessor;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.event.jobs.JobManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;

import javax.jcr.RepositoryException;
import java.util.Collections;
import java.util.Iterator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(AemContextExtension.class)
class GenerateDataFeedTest {
    private static final String JOB_MANAGER_FIELD = "jobManager";

    private static final String ADMIN_GROUP_ID = "administrators";
    private static final String TEST_GROUP_ID = "test";

    private final GenerateDataFeed fixture = new GenerateDataFeed();

    SlingHttpServletRequest requestMock;
    SlingHttpServletResponse responseMock;
    ResourceResolver resourceResolverMock;
    User userMock;
    JobManager jobManager;

    @BeforeEach
    void setup() throws NoSuchFieldException {
        requestMock = mock(SlingHttpServletRequest.class);
        responseMock = mock(SlingHttpServletResponse.class);
        resourceResolverMock = mock(ResourceResolver.class);
        userMock = mock(User.class);

        jobManager = mock(JobManager.class);
        PrivateAccessor.setField(fixture, JOB_MANAGER_FIELD, jobManager);

        when(requestMock.getResourceResolver()).thenReturn(resourceResolverMock);
        when(resourceResolverMock.adaptTo(User.class)).thenReturn(userMock);
    }

    @Test
    void testDoGet_adminUser() {
        when(userMock.isAdmin()).thenReturn(true);

        verifySlingJobAdded();
    }

    @Test
    void testDoGet_memberOfAdmin() throws RepositoryException {
        Group groupMock = mockGroup();
        when(groupMock.getID()).thenReturn(ADMIN_GROUP_ID);

        verifySlingJobAdded();
    }

    @Test
    void testDoGet_notAdmin() throws RepositoryException {
        Group groupMock = mockGroup();
        when(groupMock.getID()).thenReturn(TEST_GROUP_ID);

        verifySlingJobNotAdded();
    }

    @Test
    void testDoGet_repoException() throws RepositoryException {
        when(userMock.isAdmin()).thenReturn(false);
        when(userMock.memberOf()).thenThrow(new RepositoryException());

        verifySlingJobNotAdded();
    }

    private Group mockGroup() throws RepositoryException {
        Group groupMock = mock(Group.class);
        Iterator<Group> groups = Collections.singletonList(groupMock).iterator();

        when(userMock.isAdmin()).thenReturn(false);
        when(userMock.memberOf()).thenReturn(groups);

        return groupMock;
    }

    private void verifySlingJobAdded() {
        try (MockedStatic<SlingJobUtil> slingJobUtil = mockStatic(SlingJobUtil.class)) {
            fixture.doGet(requestMock, responseMock);

            slingJobUtil.verify(() ->
                    SlingJobUtil.addJob(any(JobManager.class), eq(DataFeedJobExecutor.GENERATE_DATA_FEED_TOPIC), anyMap())
            );
        }
    }

    private void verifySlingJobNotAdded() {
        try (MockedStatic<SlingJobUtil> slingJobUtil = mockStatic(SlingJobUtil.class)) {
            fixture.doGet(requestMock, responseMock);

            slingJobUtil.verifyNoInteractions();
        }
    }
}