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

import org.apache.commons.httpclient.HttpStatus;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a link.
 */
public final class Link {
    /**
     * Lists available types of this link.
     */
    public enum Type {
        INTERNAL("Internal"),
        EXTERNAL("External");

        private final String value;

        Type(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Address of this link
     */
    private final String href;
    /**
     * The {@link Type} is initialized based on link's address
     */
    private final Type type;
    /**
     * The status is set based on a result of checking link's validity
     */
    private LinkStatus status;

    public Link(String href, Type type) {
        this.href = href;
        this.type = type;
    }

    public String getHref() {
        return href;
    }

    public Type getType() {
        return type;
    }

    /**
     * Gets the http status code that is set based on a result of checking link's validity.
     * @return the status code if presents, or 404 otherwise
     */
    public int getStatusCode() {
        return Optional.ofNullable(status)
                .map(LinkStatus::getStatusCode)
                .orElse(HttpStatus.SC_NOT_FOUND);
    }

    /**
     * Gets the status message that is set based on a result of checking link's validity.
     * @return the status message if presents, or 404 message otherwise
     */
    public String getStatusMessage() {
        return Optional.ofNullable(status)
                .map(LinkStatus::getStatusMessage)
                .orElse(HttpStatus.getStatusText(HttpStatus.SC_NOT_FOUND));
    }

    /**
     * Gets the {@link LinkStatus} representing the status of this link
     * @return the status based on a result of checking link's validity
     */
    public LinkStatus getStatus() {
        return status;
    }

    /**
     * Sets status based on a result of checking link's validity.
     * @param status - {@link LinkStatus}
     */
    public void setStatus(LinkStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Link link = (Link) o;
        return href.equals(link.href) &&
                type.equals(link.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(href, type);
    }
}
