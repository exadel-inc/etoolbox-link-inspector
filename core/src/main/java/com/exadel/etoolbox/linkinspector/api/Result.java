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

package com.exadel.etoolbox.linkinspector.api;

import org.apache.http.ReasonPhraseCatalog;
import org.apache.http.impl.EnglishReasonPhraseCatalog;

import java.util.Locale;

/**
 * Interface representing the result of a link validation operation.
 * Provides methods to access information about a link's type, value,
 * any matched content, and the validation status.
 */
public interface Result {

    /**
     * Gets the type of the link
     *
     * @return The link type identifier
     */
    String getType();

    /**
     * Gets the value of the link
     *
     * @return The link value (URL)
     */
    String getValue();

    /**
     * Gets any matched content for the link
     *
     * @return The matched content or empty string if no match
     */
    String getMatch();

    /**
     * Gets the status of the link validation
     *
     * @return The Status object containing code and message
     */
    Status getStatus();

    /**
     * Determines if the link has been reported based on its status
     *
     * @return true if the link is not valid; false otherwise
     */
    default boolean isReported() {
        return !getStatus().isValid();
    }

    /**
     * Sets the status of the link validation
     *
     * @param status The new status to be set
     */
    void setStatus(Status status);

    /**
     * Sets the status of the link validation using a status code
     *
     * @param code The status code
     */
    default void setStatus(int code) {
        ReasonPhraseCatalog catalog = EnglishReasonPhraseCatalog.INSTANCE;
        String message = catalog.getReason(code, Locale.ENGLISH);
        setStatus(new Status(code, message));
    }

    /**
     * Sets the status of the link validation using a status message
     *
     * @param message The status message
     */
    default void setStatus(String message) {
        if (getStatus() == null) {
            setStatus(new Status(0, message));
        }
        setStatus(new Status(getStatus().getCode(), message));
    }

    /**
     * Sets the status of the link validation using both a status code and message
     *
     * @param code    The status code
     * @param message The status message
     */
    default void setStatus(int code, String message) {
        setStatus(new Status(code, message));
    }
}
