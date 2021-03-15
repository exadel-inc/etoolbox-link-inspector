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

package com.exadel.aembox.linkchecker.core.services.job;

import com.exadel.aembox.linkchecker.core.services.data.DataFeedService;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import junitx.util.PrivateAccessor;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(AemContextExtension.class)
class DataFeedJobExecutorTest {
    private static final String DATAFEED_SERVICE_FIELD = "dataFeedService";

    private final DataFeedJobExecutor fixture = new DataFeedJobExecutor();

    private DataFeedService dataFeedService;
    private Job job;
    private JobExecutionContext jobExecutionContext;
    private JobExecutionContext.ResultBuilder result;

    @BeforeEach
    void setup() throws NoSuchFieldException {
        dataFeedService = mock(DataFeedService.class);
        job = mock(Job.class);
        jobExecutionContext = mock(JobExecutionContext.class);
        result = mock(JobExecutionContext.ResultBuilder.class);
        PrivateAccessor.setField(fixture, DATAFEED_SERVICE_FIELD, dataFeedService);
    }

    @Test
    void testProcess_jobSucceeded() {
        when(jobExecutionContext.result()).thenReturn(result);

        fixture.process(job, jobExecutionContext);
        verify(dataFeedService).generateDataFeed();
        verify(result).succeeded();
    }

    @Test
    void testProcess_jobStopped() {
        when(jobExecutionContext.isStopped()).thenReturn(true);
        when(jobExecutionContext.result()).thenReturn(result);

        fixture.process(job, jobExecutionContext);
        verify(result).cancelled();
    }
}