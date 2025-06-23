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

import java.time.ZonedDateTime;

/**
 * <p><u>Note</u>: This class is not a part of the public API and is subject to change. Do not use it in your own code</p>
 * Provides generic configuration settings used by the content inspection functionality.
 * <p>
 * This interface defines methods to retrieve various configuration options that control
 * how links are discovered, filtered, and validated throughout the content repository.
 * These settings affect path inclusion/exclusion, performance controls, and validation criteria.
 */
public interface ConfigService {

    /**
     * Gets the pattern strings used to exclude links from validation based on their values.
     *
     * @return Array of regular expression patterns that match links to be excluded
     */
    String[] getExcludedLinksPatterns();

    /**
     * Gets the root path where content inspection begins.
     *
     * @return The base content path to search for links
     */
    String getSearchPath();

    /**
     * Gets the paths that should be excluded from content inspection.
     *
     * @return Array of paths to exclude from validation
     */
    String[] getExcludedPaths();

    /**
     * Determines whether to skip inspecting content that has been modified after activation.
     * This can be used to focus validation only on published content states.
     *
     * @return True if content modified after activation should be skipped, false otherwise
     */
    boolean isSkipContentModifiedAfterActivation();

    /**
     * Gets the timestamp used as a boundary for the last modification check.
     * When combined with skipContentModifiedAfterActivation, this determines
     * which content will be included based on modification date.
     *
     * @return The timestamp to use as a boundary for modification checking
     */
    ZonedDateTime getLastModified();

    /**
     * Gets the property names that should be excluded from content inspection.
     *
     * @return Array of property names to exclude from validation
     */
    String[] getExcludedProperties();

    /**
     * Determines whether links in tag properties should be excluded from validation.
     *
     * @return True if tag links should be excluded, false otherwise
     */
    boolean excludeTagLinks();

    /**
     * Gets the HTTP status codes that are considered valid during link validation.
     * Any links that return status codes not in this list will be marked as broken.
     *
     * @return Array of valid HTTP status codes
     */
    int[] getStatusCodes();

    /**
     * Gets the number of threads per CPU core to use during link validation.
     * Controls the parallelism of the validation process.
     *
     * @return The number of threads per core to use during validation
     */
    int getThreadsPerCore();
}
