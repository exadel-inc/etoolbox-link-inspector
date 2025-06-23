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
 * Represents the text fragment (such as a link) extracted from a JCR property value by a {@link Resolver}.
 * Provides methods to access information about a text fragment's type, value,
 * any matched content, and the validation status.
 */
public interface Result {

    /**
     * Gets the type of the text fragment
     *
     * @return The text fragment type identifier
     */
    String getType();

    /**
     * Gets the value of the text fragment
     *
     * @return The text fragment value (URL)
     */
    String getValue();

    /**
     * Gets any matched content for the text fragment
     *
     * @return The matched content or empty string if no match
     */
    String getMatch();

    /**
     * Gets the status of the text fragment validation
     *
     * @return The Status object containing code and message
     */
    Status getStatus();

    /**
     * Determines if the text fragment has been reported based on its status
     *
     * @return true if the text fragment is not valid; false otherwise
     */
    default boolean isReported() {
        return !getStatus().isValid();
    }

    /**
     * Sets the status of the text fragment validation
     *
     * @param status The new status to be set
     */
    void setStatus(Status status);

    /**
     * Sets the status of the text fragment validation using a status code
     *
     * @param code The status code
     */
    default void setStatus(int code) {
        ReasonPhraseCatalog catalog = EnglishReasonPhraseCatalog.INSTANCE;
        String message = catalog.getReason(code, Locale.ENGLISH);
        setStatus(new Status(code, message));
    }

    /**
     * Sets the status of the text fragment validation using a status message
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
     * Sets the status of the text fragment validation using both a status code and message
     *
     * @param code    The status code
     * @param message The status message
     */
    default void setStatus(int code, String message) {
        setStatus(new Status(code, message));
    }
}
