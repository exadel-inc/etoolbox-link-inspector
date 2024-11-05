package com.exadel.etoolbox.linkinspector.core.services.data;

import java.time.ZonedDateTime;

public interface ConfigService {
    String[] getExcludedLinksPatterns();
    String getSearchPath();
    String[] getExcludedPaths();
    boolean activatedContent();
    boolean isSkipContentModifiedAfterActivation();
    ZonedDateTime getLastModified();
    String[] getExcludedProperties();
    boolean excludeTagLinks();
    int[] getStatusCodes();
    int getThreadsPerCore();
}
