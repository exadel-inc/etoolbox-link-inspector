package com.exadel.aembox.linkchecker.core.services.helpers;

import com.exadel.aembox.linkchecker.core.models.Link;
import com.exadel.aembox.linkchecker.core.models.LinkStatus;
import com.exadel.aembox.linkchecker.core.services.ExternalLinkChecker;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.stream.Stream;

public interface LinkHelper {
    /**
     * If propertyValue is String or String[], the value is parsed based on the regex to extract links
     * which are instantiated as {@link Link} objects.
     *
     * @param propertyValue - the value to be parsed
     * @return The Stream<Link> of extract links, or Stream.empty()
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
     * Checks the given internal link validity using {@link ResourceResolver}
     *
     * @param link             - the link href to be checked
     * @param resourceResolver - {@link ResourceResolver}
     * @return the {@link LinkStatus} exposing http code and status message of the response
     */
    LinkStatus validateInternalLink(String link, ResourceResolver resourceResolver);

    /**
     * Checks the given external link validity using {@link ExternalLinkChecker}
     *
     * @param link - the link href to be checked
     * @return the {@link LinkStatus} exposing http code and status message of the response
     */
    LinkStatus validateExternalLink(String link);

    /**
     * Checks the given link validity using {@link ExternalLinkChecker} for an external
     * or {@link ResourceResolver} for an internal link.
     *
     * @param link             - the link href to be checked
     * @param resourceResolver - {@link ResourceResolver}
     * @return the {@link LinkStatus} exposing http code and status message of the response
     */
    LinkStatus validateLink(Link link, ResourceResolver resourceResolver);

    /**
     * Checks the given String link validity using {@link ExternalLinkChecker} for an external
     * or {@link ResourceResolver} for an internal link.
     *
     * @param link             - the link href to be checked
     * @param resourceResolver - {@link ResourceResolver}
     * @return the {@link LinkStatus} exposing http code and status message of the response
     */
    LinkStatus validateLink(String link, ResourceResolver resourceResolver);

    /**
     * Replaces the given link stored in the specified location (resourcePath + propertyName)
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
