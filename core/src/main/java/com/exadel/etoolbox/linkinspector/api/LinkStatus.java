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

import org.apache.http.HttpStatus;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Represents the of the {@link Link} based on a result of checking link's validity
 */
public final class LinkStatus {

    public static final LinkStatus OK = new LinkStatus(HttpStatus.SC_OK, "OK");

    private final int code;
    private final String message;

    public LinkStatus(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * Gets the status code of a link
     * @return the HTTP status code based on a result of checking link's validity
     */
    public int getCode() {
        return code;
    }

    /**
     * Gets the status message
     * @return the status message based on a result of checking link's validity
     */
    public String getMessage() {
        return message;
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
        LinkStatus that = (LinkStatus) o;
        return new EqualsBuilder().append(code, that.code).append(message, that.message).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(code).append(message).toHashCode();
    }
}
