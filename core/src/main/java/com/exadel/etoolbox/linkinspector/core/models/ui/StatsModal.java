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

import com.exadel.etoolbox.linkinspector.core.services.data.GridResourcesGenerator;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
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

    @ValueMapValue
    private String lastGenerated;

    @ValueMapValue
    private String searchPath;

    @ValueMapValue
    private String[] excludedPaths;

    @ValueMapValue
    private boolean skipModifiedAfterActivation;

    @ValueMapValue
    private String lastModifiedBoundary;

    @ValueMapValue
    private String[] excludedProperties;

    @ValueMapValue
    private String[] excludedLinksPatterns;

    @ValueMapValue
    private String excludeTags;

    @ValueMapValue
    private Integer[] allowedStatusCodes;

    @ValueMapValue
    private String[] statistics;


    public String getLastGenerated() {
        return lastGenerated;
    }

    public String getSearchPath() {
        return searchPath;
    }

    public String getExcludedPaths() {
        return arrayToStringValue(excludedPaths);
    }

    public boolean getSkipModifiedAfterActivation() {
        return skipModifiedAfterActivation;
    }

    public String getLastModifiedBoundary() {
        return lastModifiedBoundary;
    }

    public String getExcludedProperties() {
        return arrayToStringValue(excludedProperties);
    }

    public String getExcludedLinksPatterns() {
        return arrayToStringValue(excludedLinksPatterns);
    }

    public String getExcludeTags() {
        return excludeTags;
    }

    /**
     * Represents the value of the 'Status codes' field from the {@link GridResourcesGenerator} configuration used
     * during data feed generation
     *
     * @return the String comma separated representation of the specified status codes. If the status codes array
     * contains a single negative value, then this indicates that all status codes allowed.
     */
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

    public Map<String, String> getStatistics() {
        return Arrays.stream(ArrayUtils.nullToEmpty(statistics))
                .map(stat -> StringUtils.split(stat, ":"))
                .filter(statParts -> statParts.length == 2)
                .collect(Collectors.toMap(
                        statParts -> statParts[0],
                        statParts -> statParts[1],
                        (first, second) -> first,
                        LinkedHashMap::new));
    }

    /**
     * Transforms array of Strings to a single comma separated String
     * @param stringArray - the input array
     * @return comma separated representation of the array values as String
     */
    private String arrayToStringValue(String[] stringArray) {
        return Optional.ofNullable(stringArray)
                .map(array -> String.join(ARRAY_VALUES_SEPARATOR, array))
                .orElse(StringUtils.EMPTY);
    }
}