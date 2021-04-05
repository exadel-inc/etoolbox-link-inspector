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
import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

/**
 * The task for scheduled data feed generation.
 */
@Designate(ocd = DataFeedGenerationTask.Config.class)
@Component(service = Runnable.class)
public class DataFeedGenerationTask implements Runnable {

    @ObjectClassDefinition(name = "EToolbox Link Inspector - Data Feed Generation Task")
    public @interface Config {

        @AttributeDefinition(name = "Cron-job expression")
        String scheduler_expression() default "0 0 5 1/1 * ? *";

        @AttributeDefinition(name = "Concurrent task",
                description = "Whether or not to schedule this task concurrently")
        boolean scheduler_concurrent() default false;

        @AttributeDefinition(name = "Enabled",
                description = "Whether or not to enable this task")
        boolean enabled() default false;
    }

    private static final Logger LOG = LoggerFactory.getLogger(DataFeedGenerationTask.class);

    @Reference
    private JobManager jobManager;

    private boolean enabled;

    /**
     * Adds the data feed generation sling job to the ordered queue.
     */
    @Override
    public void run() {
        if (!enabled) {
            LOG.debug("The Data Feed Generation scheduled task is not enabled");
            return;
        }
        LOG.debug("The Data Feed Generation scheduled task started");
        SlingJobUtil.addJob(jobManager, DataFeedJobExecutor.GENERATE_DATA_FEED_TOPIC, Collections.emptyMap());
    }

    /**
     * Inits fields based on the service's configuration
     * @param config - the service's configuration
     */
    @Activate
    protected void activate(Config config) {
        enabled = config.enabled();
    }

    /**
     * Finds all sling jobs related to the data feed generation and stops/removes them.
     */
    @Deactivate
    protected void deactivate() {
        LOG.debug("Deactivating DataFeedGenerationTask, sling jobs with the topic {} will be stopped and removed",
                DataFeedJobExecutor.GENERATE_DATA_FEED_TOPIC);
        SlingJobUtil.stopAndRemoveJobs(jobManager, DataFeedJobExecutor.GENERATE_DATA_FEED_TOPIC);
    }
}