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

package com.exadel.aembox.linkchecker.core.services.util;

import com.exadel.aembox.linkchecker.core.models.Link;

import java.util.concurrent.atomic.AtomicInteger;

public class LinksCounter {
    private final AtomicInteger internalLinks;
    private final AtomicInteger externalLinks;

    public LinksCounter() {
        this.internalLinks = new AtomicInteger();
        this.externalLinks = new AtomicInteger();
    }

    public int getInternalLinks() {
        return internalLinks.get();
    }

    public int getExternalLinks() {
        return externalLinks.get();
    }

    public void incrementInternal() {
        this.internalLinks.incrementAndGet();
    }

    public void incrementExternal() {
        this.externalLinks.incrementAndGet();
    }

    public void countLink(Link link) {
        if (link.getType() == Link.Type.INTERNAL) {
            this.incrementInternal();
        } else if (link.getType() == Link.Type.EXTERNAL) {
            this.incrementExternal();
        }
    }
}