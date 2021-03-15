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

public class LinksCounter {
    private int internalLinks;
    private int externalLinks;

    public int getInternalLinks() {
        return internalLinks;
    }

    public int getExternalLinks() {
        return externalLinks;
    }

    public void incrementInternal() {
        this.internalLinks++;
    }

    public void incrementExternal() {
        this.externalLinks++;
    }

    public synchronized void countValidatedLinks(Link link) {
        switch (link.getType()) {
            case INTERNAL:
                this.incrementInternal();
                break;
            case EXTERNAL:
                this.incrementExternal();
        }
    }
}