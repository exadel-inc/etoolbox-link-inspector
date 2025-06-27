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

import lombok.Getter;
import org.apache.http.HttpStatus;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Represents the validation status of a text fragments, such as a link.
 * This class encapsulates the HTTP status code and message for a link validation result.
 * It provides methods to determine if a link is valid based on the status code.
 */
@Getter
public final class Status {

    public static final Status OK = new Status(HttpStatus.SC_OK, "OK");

    /**
     * Gets the status code
     */
    private final int code;

    /**
     * Gets the status message
     */
    private final String message;

    public Status(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * Indicates if a link is valid
     */
    public boolean isValid() {
        return code / 100 == 2;
    }

    @Override
    public String toString() {
        return code + StringUtils.SPACE + message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Status that = (Status) o;
        return new EqualsBuilder().append(code, that.code).append(message, that.message).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(code).append(message).toHashCode();
    }
}
