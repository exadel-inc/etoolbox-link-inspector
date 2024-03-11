package com.exadel.etoolbox.linkinspector.core.services.data.impl;

import com.exadel.etoolbox.linkinspector.core.services.data.UiConfigService;
import com.exadel.etoolbox.linkinspector.core.services.helpers.RepositoryHelper;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.Optional;

@Component(service = UiConfigService.class)
public class UiConfigServiceImpl implements UiConfigService {
    private static final String CONFIG_PATH = "/content/etoolbox-link-inspector/data/config";
    private static final String PN_FILTER = "filter";
    private static final String PN_EXCLUDED_PATHS = "excludedPaths";
    private static final String PN_PATH = "path";
    private static final String DEFAULT_PATH = "/content";

    @Reference
    private RepositoryHelper repositoryHelper;

    @Override
    public String[] getExcludedLinksPatterns() {
        return getProperty(PN_FILTER, String[].class).orElse(new String[0]);
    }

    @Override
    public String getSearchPath() {
        return getProperty(PN_PATH, String.class).orElse(DEFAULT_PATH);
    }

    @Override
    public String[] getExcludedPaths() {
        return getProperty(PN_EXCLUDED_PATHS, String[].class).orElse(new String[0]);
    }

    private <T> Optional<T> getProperty(String name, Class<T> clazz){
        try(ResourceResolver resourceResolver = repositoryHelper.getServiceResourceResolver()){
            return Optional.ofNullable(resourceResolver.getResource(CONFIG_PATH))
                    .map(Resource::getValueMap)
                    .map(vm->vm.get(name, clazz));
        }
    }
}
