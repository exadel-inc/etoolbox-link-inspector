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

@Component(service = ConfigService.class)
public class ConfigServiceImpl implements ConfigService {
    private static final String CONFIG_PATH = "/conf/etoolbox-link-inspector/data/optionsconfig";
    private static final String PN_IS_CUSTOM_TYPE_ALLOWED = "customTypeAllowed";
    private static final String PN_EXCLUDED_LINK_PATTERNS = "excludedLinkPatterns";
    private static final String PN_EXCLUDED_PATHS = "excludedPaths";
    private static final String PN_ACTIVATED_CONTENT = "activatedContent";
    private static final String PN_SKIP_CONTENT_AFTER_ACTIVATION = "skipContentAfterActivation";
    private static final String PN_LAST_MODIFIED = "lastModified";
    private static final String PN_REPLACEMENT = "replacement";
    private static final String PN_EXCLUDED_PROPERTIES = "excludedProperties";
    private static final String PN_LINKS_TYPE = "linksType";
    private static final String PN_EXCLUDE_TAGS = "excludeTags";
    private static final String PN_STATUS_CODES = "statusCodes";
    private static final String PN_THREADS_PER_CORE = "threadsPerCore";
    private static final int DEFAULT_THREADS_PER_CORE = 60;

    private static final String DEFAULT_PATH = "/content";

    @Reference
    private RepositoryHelper repositoryHelper;

    @Override
    public String[] getExcludedLinksPatterns() {
        return getProperty(PN_EXCLUDED_LINK_PATTERNS, String[].class).orElse(new String[0]);
    }

    @Override
    public String getSearchPath() {
        return getProperty(PN_REPLACEMENT, String.class).orElse(DEFAULT_PATH);
    }

    @Override
    public String[] getExcludedPaths() {
        return getProperty(PN_EXCLUDED_PATHS, String[].class).orElse(new String[0]);
    }

    @Override
    public boolean activatedContent() {
        return getProperty(PN_ACTIVATED_CONTENT, Boolean.class).orElse(false);
    }

    @Override
    public boolean isSkipContentModifiedAfterActivation() {
        return getProperty(PN_SKIP_CONTENT_AFTER_ACTIVATION, Boolean.class).orElse(false);
    }

    @Override
    public ZonedDateTime getLastModified() {
        return getProperty(PN_LAST_MODIFIED, String.class)
                .filter(StringUtils::isNotBlank)
                .map(dateString -> ZonedDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME))
                .orElse(null);
    }

    @Override
    public String[] getExcludedProperties() {
        return getProperty(PN_EXCLUDED_PROPERTIES, String[].class).orElse(new String[0]);
    }

    @Override
    public boolean isCustomTypeAllowed() {
        //TODO: check where it used
        return getProperty(PN_IS_CUSTOM_TYPE_ALLOWED, Boolean.class).orElse(false);
    }

    @Override
    public String getLinksType() {
        //TODO: check where it used and if needed replace with new config values
        return getProperty(PN_LINKS_TYPE, String.class).orElse(null);
    }

    @Override
    public boolean excludeTagLinks() {
        return getProperty(PN_EXCLUDE_TAGS, Boolean.class).orElse(true);
    }

    @Override
    public int[] getStatusCodes() {
        return Arrays.stream(getProperty(PN_STATUS_CODES, Integer[].class).orElse(new Integer[0]))
                .filter(Objects::nonNull).mapToInt(Integer::intValue).toArray();
    }

    @Override
    public int getThreadsPerCore() {
        return getProperty(PN_THREADS_PER_CORE, Integer.class).orElse(DEFAULT_THREADS_PER_CORE);
    }

    @Override
    public String getInternalLinksHost() {
        return getProperty("com.exadel.etoolbox.linkinspector.core.services.resolvers.InternalLinkResolverImpl.internalLinksHost", String.class).orElse(StringUtils.EMPTY);
    }

    @Override
    public int getConnectionTimeout() {
        return getProperty("com.exadel.etoolbox.linkinspector.core.services.resolvers.ExternalLinkResolverImpl.connectionTimeout", Integer.class).orElse(0);
    }

    @Override
    public int getSocketTimeout() {
        return getProperty("com.exadel.etoolbox.linkinspector.core.services.resolvers.ExternalLinkResolverImpl.socketTimeout", Integer.class).orElse(0);
    }

    @Override
    public String getUserAgent() {
        return getProperty("com.exadel.etoolbox.linkinspector.core.services.resolvers.ExternalLinkResolverImpl.userAgent", String.class).orElse(StringUtils.EMPTY);
    }

    private <T> Optional<T> getProperty(String name, Class<T> clazz){
        try(ResourceResolver resourceResolver = repositoryHelper.getServiceResourceResolver()){
            return Optional.ofNullable(resourceResolver.getResource(CONFIG_PATH))
                    .map(Resource::getValueMap)
                    .map(vm->vm.get(name, clazz));
        }
    }
}
