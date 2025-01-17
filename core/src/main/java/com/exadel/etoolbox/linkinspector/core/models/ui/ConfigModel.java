package com.exadel.etoolbox.linkinspector.core.models.ui;

import com.exadel.etoolbox.linkinspector.core.services.cache.GridResourcesCache;
import com.exadel.etoolbox.linkinspector.core.services.data.ConfigService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;

@Model(
        adaptables = {SlingHttpServletRequest.class, Resource.class},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class ConfigModel {

    @OSGiService
    private ConfigService configService;

    public String getSearchPath() {
        return configService.getSearchPath();
    }
}
