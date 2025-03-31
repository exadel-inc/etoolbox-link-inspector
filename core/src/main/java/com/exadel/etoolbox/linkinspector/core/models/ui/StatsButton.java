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

package com.exadel.etoolbox.linkinspector.core.models.ui;

import com.exadel.etoolbox.linkinspector.core.services.job.DataFeedJobExecutor;
import com.exadel.etoolbox.linkinspector.core.services.job.SlingJobUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import javax.inject.Inject;
import java.util.Optional;

/**
 * Represents the Stats button in the UI grid. After clicking this button, the popover represented
 * by {@link StatsModal} is displayed.
 */
@Model(
        adaptables = SlingHttpServletRequest.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class StatsButton {
    @SlingObject
    private ResourceResolver resourceResolver;

    @OSGiService
    private JobManager jobManager;

    @Inject
    private String statsResourcePath;

    /**
     * Checks existence of the resource that encloses generation statistics data.
     *
     * @return true, if the resource exists
     */
    public boolean statsResourceExists() {
        return Optional.ofNullable(statsResourcePath)
                .filter(StringUtils::isNotBlank)
                .map(resourceResolver::getResource)
                .isPresent();
    }

    public boolean isStatsAvailable() {
        return statsResourceExists() || StringUtils.contains("STARTED,ACTIVE,QUEUED,GIVEN_UP",
                SlingJobUtil.getJobStatus(jobManager, DataFeedJobExecutor.GENERATE_DATA_FEED_TOPIC)
        );
    }
}