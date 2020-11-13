package com.exadel.linkchecker.core.services.job;

import org.apache.sling.event.jobs.JobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

public class SlingJobUtil {
    private static final Logger LOG = LoggerFactory.getLogger(SlingJobUtil.class);

    private SlingJobUtil() {}

    public static void addJob(final JobManager jobManager, final String jobTopic, final Map<String, Object> payload) {
        boolean jobAdded = Optional.ofNullable(jobManager.addJob(jobTopic, payload))
                .isPresent();
        if(jobAdded) {
            LOG.info("The sling job {} was successfully added, payload {}", jobTopic, payload);
        } else {
            LOG.warn("Failed to add a sling job{}, payload {}", jobTopic, payload);
        }
    }
}