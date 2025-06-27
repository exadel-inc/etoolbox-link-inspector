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

package com.exadel.etoolbox.linkinspector.core.services.data;

/**
 * Lists the JCR property names for generation statistics data written
 * by {@link GridResourcesGenerator#generateGridResources}. Further, these property names are used to map the statistics
 * values to the {@link com.exadel.etoolbox.linkinspector.core.models.ui.StatsModal} fields
 * <p><u>Note</u>: This class is not a part of the public API and is subject to change. Do not use it in your own code</p>
 */
public final class GenerationStatsProps {
    private GenerationStatsProps() {}

    public static final String PN_LAST_GENERATED = "lastGenerated";
    public static final String PN_SEARCH_PATH = "searchPath";
    public static final String PN_EXCLUDED_PATHS = "excludedPaths";
    public static final String PN_SKIP_MODIFIED_AFTER_ACTIVATION = "skipModifiedAfterActivation";
    public static final String PN_LAST_MODIFIED_BOUNDARY = "lastModifiedBoundary";
    public static final String PN_EXCLUDED_PROPERTIES = "excludedProperties";

    public static final String PN_EXCLUDED_LINK_PATTERNS = "excludedLinksPatterns";
    public static final String PN_EXCLUDED_TAGS = "excludeTags";
    public static final String PN_ALLOWED_STATUS_CODES = "allowedStatusCodes";

    public static final String PN_STATISTICS = "statistics";
}