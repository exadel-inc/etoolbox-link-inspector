package com.exadel.aembox.linkchecker.core.schedulers;

import com.exadel.aembox.linkchecker.core.services.job.DataFeedJobExecutor;
import com.exadel.aembox.linkchecker.core.services.job.SlingJobUtil;
import junitx.util.PrivateAccessor;
import org.apache.sling.event.jobs.JobManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class DataFeedGenerationTaskTest {
    private static final String JOB_MANAGER_FIELD = "jobManager";

    private final DataFeedGenerationTask fixture = new DataFeedGenerationTask();

    @BeforeEach
    void setup() throws NoSuchFieldException {
        JobManager jobManager = mock(JobManager.class);
        PrivateAccessor.setField(fixture, JOB_MANAGER_FIELD, jobManager);
    }

    @Test
    void testRun() {
        DataFeedGenerationTask.Config config = mock(DataFeedGenerationTask.Config.class);
        when(config.enabled()).thenReturn(true);
        try (MockedStatic<SlingJobUtil> slingJobUtil = mockStatic(SlingJobUtil.class)) {
            fixture.activate(config);
            fixture.run();
            slingJobUtil.verify(() -> SlingJobUtil.addJob(any(JobManager.class), eq(DataFeedJobExecutor.GENERATE_DATA_FEED_TOPIC), anyMap()));
        }
    }

    @Test
    void testRunNotEnabled() {
        DataFeedGenerationTask.Config config = mock(DataFeedGenerationTask.Config.class);
        when(config.enabled()).thenReturn(false);
        try (MockedStatic<SlingJobUtil> slingJobUtil = mockStatic(SlingJobUtil.class)) {
            fixture.activate(config);
            fixture.run();
            slingJobUtil.verifyNoInteractions();
        }
    }

    @Test
    void testDeactivate() {
        try (MockedStatic<SlingJobUtil> slingJobUtil = mockStatic(SlingJobUtil.class)) {
            fixture.deactivate();
            slingJobUtil.verify(() -> SlingJobUtil.stopAndRemoveJobs(any(JobManager.class), eq(DataFeedJobExecutor.GENERATE_DATA_FEED_TOPIC)));
        }
    }
}