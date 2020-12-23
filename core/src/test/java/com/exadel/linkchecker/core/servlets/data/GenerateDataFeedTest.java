package com.exadel.linkchecker.core.servlets.data;

import com.exadel.linkchecker.core.services.job.DataFeedJobExecutor;
import com.exadel.linkchecker.core.services.job.SlingJobUtil;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import junitx.util.PrivateAccessor;
import org.apache.sling.event.jobs.JobManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(AemContextExtension.class)
class GenerateDataFeedTest {
    private static final String JOB_MANAGER_FIELD = "jobManager";

    private final GenerateDataFeed fixture = new GenerateDataFeed();

    @Test
    void testDoGet(AemContext context) throws NoSuchFieldException {
        JobManager jobManager = mock(JobManager.class);
        PrivateAccessor.setField(fixture, JOB_MANAGER_FIELD, jobManager);

        try (MockedStatic<SlingJobUtil> slingJobUtil = mockStatic(SlingJobUtil.class)) {
            fixture.doGet(context.request(), context.response());

            slingJobUtil.verify(() ->
                    SlingJobUtil.addJob(any(JobManager.class), eq(DataFeedJobExecutor.GENERATE_DATA_FEED_TOPIC), anyMap())
            );
        }
    }
}