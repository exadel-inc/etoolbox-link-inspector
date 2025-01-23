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

import org.apache.sling.api.resource.ResourceResolver;

import java.util.Collection;

public interface LinkResolver {

    /**
     * Gets whether the current resolver is enabled
     * @return True or false
     */
    boolean isEnabled();

    /**
     * Retrieves the token used to identify the current link type
     * @return A non-blank string value
     */
    String getId();

    /**
     * Obtains link value(s) from the given string source
     * @param source A string value. A non-blank string is expected
     * @return A collection of {@link Result} objects. Can be an empty collection, but never {@code null}
     */
    Collection<Result> getLinks(String source);

    /**
     * Validates the provided {@link Result}
     * @param result A @code Link} object; a non-null reference is expected
     * @param resourceResolver {@link ResourceResolver} object; a non-null reference is expected
     */
    void validate(Result result, ResourceResolver resourceResolver);
}
