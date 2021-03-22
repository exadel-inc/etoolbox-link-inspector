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

package com.exadel.aembox.linkchecker.core.models;

import com.google.common.collect.ImmutableList;
import org.apache.commons.httpclient.HttpStatus;

import java.util.List;

/**
 * Represents a status of the {@link Link} based on a result of checking link's validity
 */
public final class LinkStatus {
    /**
     * The range of Http status codes indicating that a link is valid
     */
    public static final List<Integer> HTTP_CODES_SUCCESS = ImmutableList.of(
            HttpStatus.SC_OK,
            HttpStatus.SC_CREATED,
            HttpStatus.SC_ACCEPTED,
            HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION,
            HttpStatus.SC_NO_CONTENT,
            HttpStatus.SC_RESET_CONTENT,
            HttpStatus.SC_PARTIAL_CONTENT,
            HttpStatus.SC_MULTI_STATUS
    );

    /**
     * Http status code
     */
    private final int statusCode;
    /**
     * Http status message
     */
    private final String statusMessage;
    /**
     * Indicates if a link is valid
     */
    private final boolean isValid;

    public LinkStatus(int statusCode, String statusMessage) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.isValid = HTTP_CODES_SUCCESS.contains(statusCode);
    }

    /**
     * Gets the status code of a link
     * @return the Http status code based on a result of checking link's validity
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Gets the status message that corresponds to the {@link #statusCode}
     * @return the status message based on a result of checking link's validity
     */
    public String getStatusMessage() {
        return statusMessage;
    }

    /**
     * Indicates if a link is valid.
     * @return true if the Http status code of a link is found in the range {@link #HTTP_CODES_SUCCESS}
     */
    public boolean isValid() {
        return isValid;
    }
}
