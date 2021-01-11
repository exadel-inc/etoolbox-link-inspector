package com.exadel.linkchecker.core.servlets.data;

import com.exadel.linkchecker.core.services.job.DataFeedJobExecutor;
import com.exadel.linkchecker.core.services.job.SlingJobUtil;
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