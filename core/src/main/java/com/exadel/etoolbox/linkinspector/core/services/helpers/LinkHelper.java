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

package com.exadel.etoolbox.linkinspector.core.services.helpers;

import com.exadel.etoolbox.linkinspector.api.Result;
import com.exadel.etoolbox.linkinspector.api.Resolver;
import com.exadel.etoolbox.linkinspector.api.Status;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.stream.Stream;

/**
 * Contains methods that assist in processing links
 * <p><u>Note</u>: This class is not a part of the public API and is subject to change. Do not use it in your own code</p>
 */
public interface LinkHelper {

    Stream<Result> getLinkStream(Object source);

    /**
     * Checks the given {@link Result} for validity using one of the registered {@link Resolver} implementations
     * @param result             - the link object to be checked
     * @param resourceResolver - {@link ResourceResolver} object
     */
    void validateLink(Result result, ResourceResolver resourceResolver);

    /**
     * Checks the given link {@code href} for validity using one of the registered {@link Resolver} implementations
     * @param link             - the string reference to be checked
     * @param resourceResolver - {@link ResourceResolver} object
     * @return A {@link Status} object
     */
    Status validateLink(String link, ResourceResolver resourceResolver);

    /**
     * Replaces all the occurrences of the given link stored at the specified location ({@code resourcePath} +
     * {@code propertyName}) with the given replacement
     * @param resourceResolver - {@link ResourceResolver}
     * @param resourcePath     - the path of the resource containing the link
     * @param propertyName     - the name of the property containing the link
     * @param currentLink      - the link to be replaced
     * @param newLink          - the replacement link
     * @return True if the replacement was successful, false otherwise
     */
    boolean replaceLink(ResourceResolver resourceResolver,
                        String resourcePath,
                        String propertyName,
                        String currentLink,
                        String newLink);
}
