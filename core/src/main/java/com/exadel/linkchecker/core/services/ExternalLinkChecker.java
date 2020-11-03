package com.exadel.linkchecker.core.services;

import java.io.IOException;
import java.net.URISyntaxException;

public interface ExternalLinkChecker {
    int checkLink(String url) throws URISyntaxException, IOException;
}