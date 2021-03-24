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

package com.exadel.etoolbox.linkinspector.core.services;

import java.io.IOException;
import java.net.URISyntaxException;

public interface ExternalLinkChecker {
    /**
     * Validates the given link via sending a HEAD request.
     *
     * @param url - the link to be checked
     * @return https status code of the response for the HEAD request
     * @throws URISyntaxException if the link has improper syntax
     * @throws IOException        in case of a problem or the connection was aborted
     */
    int checkLink(String url) throws URISyntaxException, IOException;
}