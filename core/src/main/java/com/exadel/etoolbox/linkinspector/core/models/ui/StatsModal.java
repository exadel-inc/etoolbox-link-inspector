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

import com.exadel.etoolbox.linkinspector.core.models.Link;
import com.exadel.etoolbox.linkinspector.core.services.data.GenerationStatsProps;
import com.exadel.etoolbox.linkinspector.core.services.data.GridResourcesGenerator;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Represents content of the Stats popover that contains the generation statistics data written
 * by {@link GridResourcesGenerator#generateGridResources}. Most of the
 * fields correspond to the configuration values from the
 * {@link GridResourcesGenerator} service
 */
@Model(
        adaptables = Resource.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class StatsModal {
    private static final String ARRAY_VALUES_SEPARATOR = ", ";
    private static final String ALL_STATUS_CODES_MSG = "All error codes outside the range '200-207'";

    @ValueMapValue(name = GenerationStatsProps.PN_LAST_GENERATED)
    private String lastGenerated;

    @ValueMapValue(name = GenerationStatsProps.PN_SEARCH_PATH)
    private String searchPath;

    @ValueMapValue(name = GenerationStatsProps.PN_EXCLUDED_PATHS)
    private String[] excludedPaths;

    @ValueMapValue(name = GenerationStatsProps.PN_CHECK_ACTIVATION)
    private boolean checkActivation;

    @ValueMapValue(name = GenerationStatsProps.PN_SKIP_MODIFIED_AFTER_ACTIVATION)
    private boolean skipModifiedAfterActivation;

    @ValueMapValue(name = GenerationStatsProps.PN_LAST_MODIFIED_BOUNDARY)
    private String lastModifiedBoundary;

    @ValueMapValue(name = GenerationStatsProps.PN_EXCLUDED_PROPERTIES)
    private String[] excludedProperties;

    @ValueMapValue(name = GenerationStatsProps.PN_REPORT_LINKS_TYPE)
    @Default(values = StringUtils.EMPTY)
    private String reportLinksType;

    @ValueMapValue(name = GenerationStatsProps.PN_EXCLUDED_LINK_PATTERNS)
    private String[] excludedLinksPatterns;

    @ValueMapValue(name = GenerationStatsProps.PN_EXCLUDED_TAGS)
    private String excludeTags;

    @ValueMapValue(name = GenerationStatsProps.PN_ALLOWED_STATUS_CODES)
    private Integer[] allowedStatusCodes;

    @ValueMapValue(name = GenerationStatsProps.PN_ALL_INTERNAL_LINKS)
    private String allInternalLinksCount;

    @ValueMapValue(name = GenerationStatsProps.PN_BROKEN_INTERNAL_LINKS)
    private String brokenInternalLinksCount;

    @ValueMapValue(name = GenerationStatsProps.PN_ALL_EXTERNAL_LINKS)
    private String allExternalLinksCount;

    @ValueMapValue(name = GenerationStatsProps.PN_BROKEN_EXTERNAL_LINKS)
    private String brokenExternalLinksCount;

    public String getLastGenerated() {
        return lastGenerated;
    }

    public String getSearchPath() {
        return searchPath;
    }

    public String getExcludedPaths() {
        return arrayToStringValue(excludedPaths);
    }

    public boolean getCheckActivation() {
        return checkActivation;
    }

    public boolean getSkipModifiedAfterActivation() {
        return checkActivation && skipModifiedAfterActivation;
    }

    public String getLastModifiedBoundary() {
        return lastModifiedBoundary;
    }

    public String getExcludedProperties() {
        return arrayToStringValue(excludedProperties);
    }

    /**
     * Repeats the value of the link type field from the configuration of
     * {@link GridResourcesGenerator} used during data feed generation
     *
     * @return the String representation of the selected link type. Can be either 'Internal', or 'External',
     * or 'Internal + External'
     */
    public String getReportLinksType() {
        if (StringUtils.isBlank(reportLinksType)) {
            return StringUtils.EMPTY;
        }
        return Optional.of(reportLinksType)
                .filter(GenerationStatsProps.REPORT_LINKS_TYPE_ALL::equals)
                .orElseGet(() -> Link.Type.valueOf(reportLinksType).getValue());
    }

    public String getExcludedLinksPatterns() {
        return arrayToStringValue(excludedLinksPatterns);
    }

    public String getExcludeTags() {
        return excludeTags;
    }

    public String getAllowedStatusCodes() {
        if (allowedStatusCodes == null) {
            return StringUtils.EMPTY;
        }
        boolean isAllCodesAllowed = Optional.of(allowedStatusCodes)
                .filter(statusCodes -> statusCodes.length == 1)
                .filter(statusCodes -> statusCodes[0] < 0)
                .isPresent();
        if (isAllCodesAllowed) {
            return ALL_STATUS_CODES_MSG;
        } else {
            return Arrays.stream(allowedStatusCodes)
                    .map(String::valueOf)
                    .collect(Collectors.joining(ARRAY_VALUES_SEPARATOR));
        }
    }

    /**
     * Gets the count of all the inspected internal links
     *
     * @return the count of all internal links or empty String, if the report contains 'External' link only
     */
    public String getAllInternalLinksCount() {
        if (Link.Type.EXTERNAL.getValue().equalsIgnoreCase(reportLinksType)) {
            return StringUtils.EMPTY;
        }
        return allInternalLinksCount;
    }

    public String getBrokenInternalLinksCount() {
        return brokenInternalLinksCount;
    }

    /**
     * Gets the count of all the inspected external links
     *
     * @return the count of all external links or empty String, if the report contains 'Internal' link only
     */
    public String getAllExternalLinksCount() {
        if (Link.Type.INTERNAL.getValue().equalsIgnoreCase(reportLinksType)) {
            return StringUtils.EMPTY;
        }
        return allExternalLinksCount;
    }

    public String getBrokenExternalLinksCount() {
        return brokenExternalLinksCount;
    }

    private String arrayToStringValue(String[] stringArray) {
        return Optional.ofNullable(stringArray)
                .map(array -> String.join(ARRAY_VALUES_SEPARATOR, array))
                .orElse(StringUtils.EMPTY);
    }
}