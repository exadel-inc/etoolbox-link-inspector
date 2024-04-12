package com.exadel.etoolbox.linkinspector.core.services.data;

import java.time.ZonedDateTime;

public interface UiConfigService {
    String[] getExcludedLinksPatterns();
    String getSearchPath();
    String[] getExcludedPaths();
    boolean isActivatedContent();
    boolean isSkipContentModifiedAfterActivation();
    ZonedDateTime getLastModified();
    String[] getExcludedProperties();
    String getLinksType();
    boolean allowedCustomLinkType();
    boolean isExcludeTags();
    int[] getStatusCodes();
    int getThreadsPerCore();
}
