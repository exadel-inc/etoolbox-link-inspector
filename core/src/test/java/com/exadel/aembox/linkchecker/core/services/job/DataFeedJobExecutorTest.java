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