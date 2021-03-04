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
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobExecutionContext;
import org.apache.sling.event.jobs.consumer.JobExecutionResult;
import org.apache.sling.event.jobs.consumer.JobExecutor;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = JobExecutor.class,
        property = {
                JobExecutor.PROPERTY_TOPICS + "=" + DataFeedJobExecutor.GENERATE_DATA_FEED_TOPIC
        })
public class DataFeedJobExecutor implements JobExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(DataFeedJobExecutor.class);

    public static final String GENERATE_DATA_FEED_TOPIC = "aembox/linkchecker/job/datafeed/generate";

    @Reference
    private DataFeedService dataFeedService;

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