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

public final class LinkStatus {
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

    private final int statusCode;
    private final String statusMessage;
    private final boolean isValid;

    public LinkStatus(int statusCode, String statusMessage) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.isValid = HTTP_CODES_SUCCESS.contains(statusCode);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public boolean isValid() {
        return isValid;
    }
}
