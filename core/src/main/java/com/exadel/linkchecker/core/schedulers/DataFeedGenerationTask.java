package com.exadel.linkchecker.core.schedulers;

import com.exadel.linkchecker.core.services.job.DataFeedJobExecutor;
import com.exadel.linkchecker.core.services.job.SlingJobUtil;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

@Designate(ocd = DataFeedGenerationTask.Config.class)
@Component(service = Runnable.class)
public class DataFeedGenerationTask implements Runnable {

    @ObjectClassDefinition(name="Exadel Link Checker - Data Feed Generation Task")
    public static @interface Config {

        @AttributeDefinition(name = "Cron-job expression")
        String scheduler_expression() default "0 0 5 1/1 * ? *";

        @AttributeDefinition(name = "Concurrent task",
                             description = "Whether or not to schedule this task concurrently")
        boolean scheduler_concurrent() default false;

        @AttributeDefinition(name = "Enabled",
                             description = "Whether or not to enable this task")
        boolean enabled() default false;
    }

    private final Logger LOG = LoggerFactory.getLogger(DataFeedGenerationTask.class);

    @Reference
    private JobManager jobManager;

    private boolean enabled;
    
    @Override
    public void run() {
        if (!enabled) {
            LOG.debug("The Data Feed Generation scheduled task is not enabled");
            return;
        }
        LOG.debug("The Data Feed Generation scheduled task started");
        SlingJobUtil.addJob(jobManager, DataFeedJobExecutor.GENERATE_DATA_FEED_TOPIC, Collections.emptyMap());
    }

    @Activate
    protected void activate(final Config config) {
        enabled = config.enabled();
    }
}