package com.exadel.linkchecker.core.models.ui;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import javax.inject.Inject;
import java.util.Optional;

/**
 * The model representing the stats button.
 */
@Model(
        adaptables = SlingHttpServletRequest.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class StatsButton {
    @SlingObject
    ResourceResolver resourceResolver;

    @Inject
    String statsResourcePath;

    public boolean isStatsResourceExist() {
        return Optional.ofNullable(statsResourcePath)
                .filter(StringUtils::isNotBlank)
                .map(resourceResolver::getResource)
                .isPresent();
    }
}