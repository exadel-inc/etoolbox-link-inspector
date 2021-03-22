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

import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public class SlingJobUtil {
    private SlingJobUtil() {}

    private static final Logger LOG = LoggerFactory.getLogger(SlingJobUtil.class);

    public static void addJob(JobManager jobManager, String jobTopic, Map<String, Object> payload) {
        boolean jobAdded = Optional.ofNullable(jobManager.addJob(jobTopic, payload))
                .isPresent();
        if (jobAdded) {
            LOG.info("The sling job {} was successfully added, payload {}", jobTopic, payload);
        } else {
            LOG.warn("Failed to add a sling job{}, payload {}", jobTopic, payload);
        }
    }

    @SuppressWarnings("unchecked")
    public static void stopAndRemoveJobs(JobManager jobManager, String topic) {
        Collection<Job> jobs = jobManager.findJobs(JobManager.QueryType.ALL, topic, -1);
        if (!jobs.isEmpty()) {
            LOG.debug("The sling jobs to be stopped and removed: {}", jobs);
            jobs.forEach(job -> {
                String jobId = job.getId();
                jobManager.stopJobById(jobId);
                boolean isRemoved = jobManager.removeJobById(jobId);
                LOG.info("The sling job with topic:{} and id: {} was stopped and removed: {}", topic, jobId, isRemoved);
            });
        } else {
            LOG.debug("No sling jobs with the topic {} were found", topic);
        }
    }
}