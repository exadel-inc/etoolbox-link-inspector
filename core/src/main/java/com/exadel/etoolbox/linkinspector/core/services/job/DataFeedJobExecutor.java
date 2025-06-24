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

package com.exadel.etoolbox.linkinspector.core.services.job;

import com.exadel.etoolbox.linkinspector.core.services.data.DataFeedService;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobExecutionContext;
import org.apache.sling.event.jobs.consumer.JobExecutionResult;
import org.apache.sling.event.jobs.consumer.JobExecutor;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p><u>Note</u>: This class is not a part of the public API and is subject to change. Do not use it in your own code</p>
 * Handles the asynchronous generation of the link data feed via a Sling job.
 * <p>
 * This class consumes jobs on the data feed generation topic and delegates the
 * actual data feed creation to the {@link DataFeedService}. Using Sling jobs allows
 * for decoupling the generation request from the actual processing, enabling
 * long-running tasks to execute in the background without blocking user interactions.
 * <p>
 * The executor registers itself for the {@link #GENERATE_DATA_FEED_TOPIC} topic and
 * processes any jobs posted to that topic.
 */
@Component(service = JobExecutor.class,
        property = {
                JobExecutor.PROPERTY_TOPICS + "=" + DataFeedJobExecutor.GENERATE_DATA_FEED_TOPIC
        })
public class DataFeedJobExecutor implements JobExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(DataFeedJobExecutor.class);

    public static final String GENERATE_DATA_FEED_TOPIC = "etoolbox/link-inspector/job/datafeed/generate";

    @Reference
    private DataFeedService dataFeedService;

    /**
     * Processes a data feed generation job.
     * <p>
     * This method is called by the Sling job manager when a job with the
     * {@link #GENERATE_DATA_FEED_TOPIC} topic is ready to be processed. It delegates
     * the actual work to the {@link DataFeedService#generateDataFeed()} method.
     * <p>
     * The method handles job cancellation by checking if the job execution context
     * has been stopped during processing.
     *
     * @param job The job to process
     * @param jobExecutionContext Context for the job execution
     * @return A job execution result indicating success, failure, or cancellation
     */
    @Override
    public JobExecutionResult process(Job job, JobExecutionContext jobExecutionContext) {
        LOG.debug("DataFeedJobExecutor - start Data Feed Generation sling job processing");
        dataFeedService.generateDataFeed();
        if (jobExecutionContext.isStopped()) {
            LOG.debug("DataFeedJobExecutor - Data Feed Generation sling job cancelled");
            return jobExecutionContext.result().cancelled();
        }
        LOG.debug("DataFeedJobExecutor - Data Feed Generation sling job completed");
        return jobExecutionContext.result().succeeded();
    }
}