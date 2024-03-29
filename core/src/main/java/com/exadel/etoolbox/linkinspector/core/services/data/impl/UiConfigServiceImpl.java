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

    @Reference
    private RepositoryHelper repositoryHelper;

    @Override
    public String[] getExcludedLinksPatterns() {
        try(ResourceResolver resourceResolver = repositoryHelper.getServiceResourceResolver()){
            return Optional.ofNullable(resourceResolver.getResource(CONFIG_PATH))
                    .map(Resource::getValueMap)
                    .map(vm->vm.get(PN_FILTER, String[].class))
                    .orElse(new String[0]);
        }
    }
}
