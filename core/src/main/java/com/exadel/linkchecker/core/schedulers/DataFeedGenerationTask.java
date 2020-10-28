package com.exadel.linkchecker.core.schedulers;

import com.exadel.linkchecker.core.services.data.DataFeedService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Designate(ocd = DataFeedGenerationTask.Config.class)
@Component(service = Runnable.class)
public class DataFeedGenerationTask implements Runnable {

    @ObjectClassDefinition(name="Exadel Link Checker - data feed generation scheduled task",
                           description = "Simple demo for cron-job like task with properties")
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
    private DataFeedService dataFeedService;

    private boolean enabled;
    
    @Override
    public void run() {
        if (!enabled) {
            LOG.debug("The Data feed generation scheduled task is not enabled");
            return;
        }
        dataFeedService.generateDataFeed();
    }

    @Activate
    protected void activate(final Config config) {
        enabled = config.enabled();
    }
}