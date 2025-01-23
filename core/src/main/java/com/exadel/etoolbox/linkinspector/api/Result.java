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

import org.eclipse.jetty.http.HttpStatus;

public interface Result {

    String getType();

    String getValue();

    String getMatch();

    LinkStatus getStatus();

    default boolean isReported() {
        return !getStatus().isValid();
    }

    void setStatus(LinkStatus status);

    default void setStatus(int code) {
        setStatus(new LinkStatus(code, HttpStatus.getMessage(code)));
    }

    default void setStatus(String message) {
        if (getStatus() == null) {
            setStatus(new LinkStatus(0, message));
        }
        setStatus(new LinkStatus(getStatus().getCode(), message));
    }

    default void setStatus(int code, String message) {
        setStatus(new LinkStatus(code, message));
    }
}
