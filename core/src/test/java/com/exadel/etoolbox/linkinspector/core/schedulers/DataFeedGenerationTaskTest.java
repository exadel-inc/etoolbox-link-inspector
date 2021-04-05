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

package com.exadel.etoolbox.linkinspector.core.schedulers;

import com.exadel.etoolbox.linkinspector.core.services.job.DataFeedJobExecutor;
import com.exadel.etoolbox.linkinspector.core.services.job.SlingJobUtil;
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