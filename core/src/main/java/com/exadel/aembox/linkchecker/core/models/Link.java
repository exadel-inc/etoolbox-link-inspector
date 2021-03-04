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

public final class Link {
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

    private final String href;
    private final Type type;
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

    public int getStatusCode() {
        return Optional.ofNullable(status)
                .map(LinkStatus::getStatusCode)
                .orElse(HttpStatus.SC_NOT_FOUND);
    }

    public String getStatusMessage() {
        return Optional.ofNullable(status)
                .map(LinkStatus::getStatusMessage)
                .orElse(HttpStatus.getStatusText(HttpStatus.SC_NOT_FOUND));
    }

    public LinkStatus getStatus() {
        return status;
    }

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
