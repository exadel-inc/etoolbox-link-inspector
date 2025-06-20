package com.exadel.etoolbox.linkinspector.core.services.data.impl;

import com.day.cq.commons.jcr.JcrConstants;
import com.exadel.etoolbox.linkinspector.core.services.helpers.RepositoryHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

/**
 * Listens to changes in configuration resources and updates OSGi configurations accordingly.
 * This component observes resource changes (add, change, remove) in the configuration path
 * and synchronizes them with the OSGi ConfigurationAdmin service.
 */
@Component(
        service = ResourceChangeListener.class,
        immediate = true,
        property = {
                ResourceChangeListener.PATHS + "=" + ConfigServiceImpl.CONFIG_PATH,
                ResourceChangeListener.CHANGES + "=ADDED",
                ResourceChangeListener.CHANGES + "=CHANGED",
                ResourceChangeListener.CHANGES + "=REMOVED"
        }
)
@Slf4j
public class ConfigResourceListenerImpl implements ResourceChangeListener {

    private static final String INTERNAL_PACKAGE = "com.exadel.etoolbox.linkinspector";
    private static final String UPDATABLE_CONFIG_TOKEN = "?";

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

    /**
     * Handles resource change events
     *
     * @param list List of resource changes to process
     */
    @Override
    public void onChange(List<ResourceChange> list) {
        try (ResourceResolver resolver = repositoryHelper.getServiceResourceResolver()) {
            for (ResourceChange change : list) {
                if (change.getPath().equals(ConfigServiceImpl.CONFIG_PATH)) {
                    // Skip the root path change
                    continue;
                }
                String pid = StringUtils.substringAfterLast(change.getPath(), "/");
                if (change.getType() == ResourceChange.ChangeType.REMOVED) {
                    deleteConfiguration(pid);
                    continue;
                }
                Resource node = resolver.getResource(change.getPath());
                if (node == null) {
                    deleteConfiguration(pid);
                    continue;
                }
                updateConfiguration(node);
            }
        }
    }

    private Configuration getConfiguration(String pid) {
        Configuration result = null;
        try {
            result = configurationAdmin.getConfiguration(pid);
            if (result == null) {
                log.warn("Configuration for pid {} is not found", pid);
            }
        } catch (Exception e) {
            log.error("Failed to get configuration for pid {}", pid, e);
        }
        return result;
    }

    private void deleteConfiguration(String pid) {
        Configuration configuration = getConfiguration(pid);
        if (configuration == null) {
            return;
        }
        try {
            configuration.delete();
        } catch (Exception e) {
            log.error("Failed to delete configuration for pid {}", pid, e);
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
        boolean isForeignConfig = !node.getName().startsWith(INTERNAL_PACKAGE);
        String bundleLocation = configuration.getBundleLocation();
        if (StringUtils.isNotEmpty(bundleLocation) && isForeignConfig && !bundleLocation.startsWith(UPDATABLE_CONFIG_TOKEN)) {
            configuration.setBundleLocation(UPDATABLE_CONFIG_TOKEN + bundleLocation);
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

    private static Dictionary<String, ?> toDictionary(ValueMap properties) {
        Dictionary<String, Object> dictionary = new Hashtable<>();
        properties.entrySet()
                .stream()
                .filter(entry -> !StringUtils.startsWithAny(entry.getKey(), "cq:", "granite:", "jcr:", "sling:"))
                .filter(entry -> isValidDictionaryEntry(entry.getValue()))
                .forEach(entry -> dictionary.put(entry.getKey(), entry.getValue()));
        return dictionary;
    }

    private static boolean isValidDictionaryEntry(Object property) {
        if (property == null) {
            return false;
        }
        if (property instanceof Array) {
            return isValidDictionaryType(property.getClass().getComponentType());
        }
        if (property instanceof Collection) {
            return ((Collection<?>) property).stream().allMatch(e -> e != null && isValidDictionaryEntry(e.getClass()));
        }
        return isValidDictionaryType(property.getClass());
    }

    private static boolean isValidDictionaryType(Class<?> type) {
        return ClassUtils.isPrimitiveOrWrapper(type) || String.class.equals(type);
    }
}
