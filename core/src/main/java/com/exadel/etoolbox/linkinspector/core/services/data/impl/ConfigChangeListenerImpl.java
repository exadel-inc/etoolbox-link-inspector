package com.exadel.etoolbox.linkinspector.core.services.data.impl;

import com.day.cq.commons.jcr.JcrConstants;
import com.exadel.etoolbox.linkinspector.core.services.helpers.RepositoryHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

@Component(
        service = ResourceChangeListener.class,
        immediate = true,
        property = {
                ResourceChangeListener.PATHS + "=/conf/etoolbox/link-inspector",
                ResourceChangeListener.CHANGES + "=ADDED",
                ResourceChangeListener.CHANGES + "=CHANGED",
                ResourceChangeListener.CHANGES + "=REMOVED"
        }
)
@Slf4j
public class ConfigChangeListenerImpl implements ResourceChangeListener {

    @Reference
    private transient ConfigurationAdmin configurationAdmin;

    @Reference
    private transient RepositoryHelper repositoryHelper;

    @Activate
    private void activate() {
        try (ResourceResolver resolver = repositoryHelper.getServiceResourceResolver()) {
            Resource configRoot = resolver.getResource(ConfigServiceImpl.CONFIG_PATH);
            if (configRoot == null) {
                log.info("Configuration root not found");
                return;
            }
            for (Resource node : configRoot.getChildren()) {
                updateConfiguration(node);
            }
        }
    }

    @Override
    public void onChange(@NotNull List<ResourceChange> list) {
        try (ResourceResolver resolver = repositoryHelper.getServiceResourceResolver()) {
            for (ResourceChange change : list) {
                Resource node = resolver.getResource(change.getPath());
                if (node == null) {
                    log.error("Failed to get properties for pid {}", StringUtils.substringAfterLast(change.getPath(), "/"));
                    return;
                }
                updateConfiguration(node);
            }
        }
    }

    private void updateConfiguration(Resource node) {
        if (node == null || !JcrConstants.NT_UNSTRUCTURED.equals(node.getResourceType())) {
            return;
        }
        Configuration configuration = getConfiguration(node.getName());
        if (configuration == null) {
            return;
        }
        updateConfiguration(configuration, node.getValueMap());
    }

    private void updateConfiguration(Configuration configuration, ValueMap properties) {
        Dictionary<String, ?> dictionary = toDictionary(properties);
        try {
            configuration.update(dictionary);
        } catch (Exception e) {
            log.error("Failed to update configuration for pid {}", configuration.getPid(), e);
        }
    }

    private Configuration getConfiguration(String pid) {
        try {
            return configurationAdmin.getConfiguration(pid);
        } catch (IOException e) {
            log.error("Failed to get configuration for pid {}", pid, e);
        }
        return null;
    }

    private static Dictionary<String, ?> toDictionary(ValueMap properties) {
        Dictionary<String, Object> dictionary = new Hashtable<>();
        properties.entrySet()
                .stream()
                .filter(entry -> !entry.getKey().contains(":") && entry.getValue() != null)
                .forEach(entry -> dictionary.put(entry.getKey(), entry.getValue()));
        return dictionary;
    }
}
