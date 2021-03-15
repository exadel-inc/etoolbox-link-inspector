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

package com.exadel.aembox.linkchecker.core.services.data;

public final class GenerationStatsProps {
    private GenerationStatsProps() {}

    public static final String PN_LAST_GENERATED = "lastGenerated";
    public static final String PN_SEARCH_PATH = "searchPath";
    public static final String PN_EXCLUDED_PATHS = "excludedPaths";
    public static final String PN_CHECK_ACTIVATION = "checkActivation";
    public static final String PN_SKIP_MODIFIED_AFTER_ACTIVATION = "skipModifiedAfterActivation";
    public static final String PN_LAST_MODIFIED_BOUNDARY = "lastModifiedBoundary";
    public static final String PN_EXCLUDED_PROPERTIES = "excludedProperties";

    public static final String PN_REPORT_LINKS_TYPE = "reportLinksType";
    public static final String PN_EXCLUDED_LINK_PATTERNS = "excludedLinksPatterns";
    public static final String PN_EXCLUDED_TAGS = "excludeTags";
    public static final String PN_ALLOWED_STATUS_CODES = "allowedStatusCodes";

    public static final String PN_ALL_INTERNAL_LINKS = "allInternalLinks";
    public static final String PN_BROKEN_INTERNAL_LINKS = "brokenInternalLinks";
    public static final String PN_ALL_EXTERNAL_LINKS = "allExternalLinks";
    public static final String PN_BROKEN_EXTERNAL_LINKS = "brokenExternalLinks";

    public static final String REPORT_LINKS_TYPE_ALL = "Internal + External";
}