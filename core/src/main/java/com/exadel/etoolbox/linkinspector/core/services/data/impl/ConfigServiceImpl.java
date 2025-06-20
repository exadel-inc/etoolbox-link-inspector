package com.exadel.etoolbox.linkinspector.core.services.data.impl;

import com.exadel.etoolbox.linkinspector.core.services.data.ConfigService;
import com.exadel.etoolbox.linkinspector.core.services.helpers.RepositoryHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementation of the ConfigService interface that provides configuration settings
 * for the Link Inspector tool. This service retrieves configuration values from the
 * repository and provides default values when configurations are not defined.
 */
@Component(service = ConfigService.class)
public class ConfigServiceImpl implements ConfigService {
    static final String CONFIG_PATH = "/conf/etoolbox/link-inspector";
    private static final String PN_EXCLUDED_LINK_PATTERNS = "excludedLinkPatterns";
    private static final String PN_EXCLUDED_PATHS = "excludedPaths";
    private static final String PN_SKIP_CONTENT_AFTER_ACTIVATION = "skipContentAfterActivation";
    private static final String PN_ENABLED_LAST_MODIFIED = "enableLastModified";
    private static final String PN_LAST_MODIFIED = "lastModified";
    private static final String PN_PATH = "path";
    private static final String PN_EXCLUDED_PROPERTIES = "excludedProperties";
    private static final String PN_EXCLUDE_TAGS = "excludeTags";
    private static final String PN_STATUS_CODES = "statusCodes";
    private static final String PN_THREADS_PER_CORE = "threadsPerCore";
    private static final int DEFAULT_THREADS_PER_CORE = 60;

    private static final String DEFAULT_PATH = "/content";

    @Reference
    private RepositoryHelper repositoryHelper;

    /**
     * Returns patterns for links that should be excluded from processing
     *
     * @return Array of regex patterns for links that should be excluded
     */
    @Override
    public String[] getExcludedLinksPatterns() {
        return getProperty(PN_EXCLUDED_LINK_PATTERNS, String[].class).orElse(new String[0]);
    }

    /**
     * Returns the configured content path that should be searched for links
     *
     * @return The content path to search, or the default path if not configured
     */
    @Override
    public String getSearchPath() {
        return getProperty(PN_PATH, String.class).orElse(DEFAULT_PATH);
    }

    /**
     * Returns paths that should be excluded from the link inspection
     *
     * @return Array of paths to be excluded from processing
     */
    @Override
    public String[] getExcludedPaths() {
        return getProperty(PN_EXCLUDED_PATHS, String[].class).orElse(new String[0]);
    }

    /**
     * Checks if the content modification after activation should be skipped
     *
     * @return true if content modification after activation is to be skipped, false otherwise
     */
    @Override
    public boolean isSkipContentModifiedAfterActivation() {
        return getProperty(PN_SKIP_CONTENT_AFTER_ACTIVATION, Boolean.class).orElse(false);
    }

    /**
     * Returns the last modified date and time for the content, if enabled
     *
     * @return The last modified date and time, or null if not enabled or not available
     */
    @Override
    public ZonedDateTime getLastModified() {
        if (!getProperty(PN_ENABLED_LAST_MODIFIED, Boolean.class).orElse(false)) {
            return null;
        }
        return getProperty(PN_LAST_MODIFIED, String.class)
                .filter(StringUtils::isNotBlank)
                .map(dateString -> ZonedDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME))
                .orElse(null);
    }

    /**
     * Returns properties that should be excluded from the link inspection
     *
     * @return Array of property names to be excluded from processing
     */
    @Override
    public String[] getExcludedProperties() {
        return getProperty(PN_EXCLUDED_PROPERTIES, String[].class).orElse(new String[0]);
    }

    /**
     * Checks if links with excluded tags should be ignored
     *
     * @return true if links with excluded tags should be ignored, false otherwise
     */
    @Override
    public boolean excludeTagLinks() {
        return getProperty(PN_EXCLUDE_TAGS, Boolean.class).orElse(true);
    }

    /**
     * Returns the HTTP status codes that are considered in the link inspection
     *
     * @return Array of HTTP status codes
     */
    @Override
    public int[] getStatusCodes() {
        return Arrays.stream(getProperty(PN_STATUS_CODES, Integer[].class).orElse(new Integer[0]))
                .filter(Objects::nonNull).mapToInt(Integer::intValue).toArray();
    }

    /**
     * Returns the number of threads to be used per core for processing
     *
     * @return The number of threads per core
     */
    @Override
    public int getThreadsPerCore() {
        return getProperty(PN_THREADS_PER_CORE, Integer.class).orElse(DEFAULT_THREADS_PER_CORE);
    }

    private <T> Optional<T> getProperty(String name, Class<T> clazz){
        try(ResourceResolver resourceResolver = repositoryHelper.getServiceResourceResolver()){
            return Optional.ofNullable(resourceResolver.getResource(CONFIG_PATH))
                    .map(Resource::getValueMap)
                    .map(vm->vm.get(name, clazz));
        }
    }
}
