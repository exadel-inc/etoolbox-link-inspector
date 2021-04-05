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

import com.exadel.etoolbox.linkinspector.core.models.Link;
import com.exadel.etoolbox.linkinspector.core.models.LinkStatus;
import com.exadel.etoolbox.linkinspector.core.services.ExternalLinkChecker;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.stream.Stream;

/**
 * Contains methods that assist in links processing.
 */
public interface LinkHelper {
    /**
     * Parses JCR property value to fetch links based on regex. Applies to String or String[] property types only.
     * The extracted links are instantiated as {@link Link} objects.
     *
     * @param propertyValue - the JCR property value to be parsed
     * @return The Stream<Link> of extracted links, or Stream.empty()
     */
    Stream<Link> getLinkStreamFromProperty(Object propertyValue);

    /**
     * Extracts external links based on regex from the string value
     *
     * @param text - the string where links are extracted from
     * @return The Stream<String> of extracted links
     */
    Stream<String> getExternalLinksFromString(String text);

    /**
     * Extracts internal links based on regex from the string value
     *
     * @param text - the string where links are extracted from
     * @return The Stream<String> of extracted links
     */
    Stream<String> getInternalLinksFromString(String text);

    /**
     * Checks the given internal link validity using {@link ResourceResolver}. Basically the existence of a resource
     * in the repository is checked
     *
     * @param link             - the link href to be checked
     * @param resourceResolver - {@link ResourceResolver}
     * @return the {@link LinkStatus} representing http code and status message of the response
     */
    LinkStatus validateInternalLink(String link, ResourceResolver resourceResolver);

    /**
     * Checks the given external link validity using {@link ExternalLinkChecker}
     *
     * @param link - the link href to be checked
     * @return the {@link LinkStatus} representing http code and status message of the response
     */
    LinkStatus validateExternalLink(String link);

    /**
     * Checks the given link validity using {@link ExternalLinkChecker} for an external link
     * or {@link ResourceResolver} for an internal link.
     *
     * @param link             - the link href to be checked
     * @param resourceResolver - {@link ResourceResolver}
     * @return the {@link LinkStatus} representing http code and status message of the response
     */
    LinkStatus validateLink(Link link, ResourceResolver resourceResolver);

    /**
     * Checks the given String link validity using {@link ExternalLinkChecker} for an external link
     * or {@link ResourceResolver} for an internal link.
     *
     * @param link             - the link href to be checked
     * @param resourceResolver - {@link ResourceResolver}
     * @return the {@link LinkStatus} representing http code and status message of the response
     */
    LinkStatus validateLink(String link, ResourceResolver resourceResolver);

    /**
     * Replaces the given link stored at the specified location (resourcePath + propertyName)
     * with the given replacement.
     * <p>
     * All the links contained within the specified property are retrieved
     * by {@link #getLinkStreamFromProperty}. If none matches the given link, no replacement is applied.
     * Otherwise, all matching links are replaced with the given replacement.
     *
     * @param resourceResolver - {@link ResourceResolver}
     * @param resourcePath     - the path of the resource containing the link
     * @param propertyName     - the name of the property containing the link
     * @param currentLink      - the link to be replaced
     * @param newLink          - the replacement link
     * @return true, if the replacement completed successfully
     */
    boolean replaceLink(ResourceResolver resourceResolver, String resourcePath, String propertyName,
                        String currentLink, String newLink);
}
