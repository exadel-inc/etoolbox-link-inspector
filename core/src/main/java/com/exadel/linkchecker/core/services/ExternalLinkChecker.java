package com.exadel.linkchecker.core.services;

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