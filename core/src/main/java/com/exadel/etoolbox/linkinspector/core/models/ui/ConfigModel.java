package com.exadel.etoolbox.linkinspector.core.models.ui;

import com.exadel.etoolbox.linkinspector.core.services.cache.GridResourcesCache;
import com.exadel.etoolbox.linkinspector.core.services.data.ConfigService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;

/**
 * Provides configuration data to UI components.
 * This model exposes configuration settings from the ConfigService for use in the frontend.
 * <p><u>Note</u>: This class is not a part of the public API and is subject to change. Do not use it in your own code</p>
 */
@Model(
        adaptables = {SlingHttpServletRequest.class, Resource.class},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class ConfigModel {

    @OSGiService
    private ConfigService configService;

    /**
     * Returns the configured search path for the Link Inspector tool
     *
     * @return The content path that will be searched for links
     */
    public String getSearchPath() {
        return configService.getSearchPath();
    }
}
