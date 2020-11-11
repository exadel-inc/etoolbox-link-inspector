package com.exadel.linkchecker.core.services;

import com.day.cq.rewriter.linkchecker.ExternalLinkChecker;
import com.exadel.linkchecker.core.models.Link;
import com.exadel.linkchecker.core.models.LinkStatus;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.stream.Stream;

public interface LinkHelper {
    /**
     * If propertyValue is String or String[], the value is parsed based on the regex to fetch links
     * which are instantiated as {@link Link} objects.
     *
     * @param propertyValue - the value to be parsed
     * @return The Stream<Link> of parsed links, or Stream.empty()
     */
    Stream<Link> getLinkStream(Object propertyValue);

    Stream<String> getExternalLinksFromString(String text);

    Stream<String> getInternalLinksFromString(String text);

    LinkStatus validateInternalLink(String link, ResourceResolver resourceResolver);

    /**
     * Checks the given link validity based on OOTB {@link ExternalLinkChecker}
     *
     * @param link - to be checked
     * @return the http status response
     */
    LinkStatus validateExternalLink(String link);

    LinkStatus validateLink(Link link, ResourceResolver resourceResolver);
}
