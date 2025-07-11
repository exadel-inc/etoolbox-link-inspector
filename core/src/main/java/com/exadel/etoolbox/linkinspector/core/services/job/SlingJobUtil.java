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

import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Utility class for working with Sling Jobs.
 * Provides helper methods for adding, finding, and checking the status of Sling jobs.
 * <p><u>Note</u>: This class is not a part of the public API and is subject to change. Do not use it in your own code</p>
 */
public class SlingJobUtil {
    private SlingJobUtil() {}

    private static final Logger LOG = LoggerFactory.getLogger(SlingJobUtil.class);

    /**
     * Adds a new job to the Sling Job Manager
     *
     * @param jobManager The Sling JobManager service
     * @param jobTopic The topic for the job
     * @param payload Map of properties to include with the job
     */
    public static void addJob(JobManager jobManager, String jobTopic, Map<String, Object> payload) {
        boolean jobAdded = Optional.ofNullable(jobManager.addJob(jobTopic, payload))
                .isPresent();
        if (jobAdded) {
            LOG.info("The sling job {} was successfully added, payload {}", jobTopic, payload);
        } else {
            LOG.warn("Failed to add a sling job{}, payload {}", jobTopic, payload);
        }
    }

    /**
     * Stops and removes jobs with the given topic
     *
     * @param jobManager The Sling JobManager service
     * @param topic The topic of the jobs to stop and remove
     */
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

    /**
     * Gets the status of the job with the given topic
     *
     * @param jobManager The Sling JobManager service
     * @param topic The topic of the job whose status is to be checked
     * @return The status of the job, or STOPPED if no job with the given topic is found
     */
    public static String getJobStatus(JobManager jobManager, String topic) {
        Collection<Job> jobs = jobManager.findJobs(JobManager.QueryType.ALL, topic, -1);
        if (!jobs.isEmpty()) {
            return jobs.stream().findFirst().get().getJobState().toString();
        }
        return Job.JobState.STOPPED.toString();
    }
}