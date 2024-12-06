package com.exadel.etoolbox.linkinspector.core.services.util;

import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.day.cq.commons.jcr.JcrConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class GraniteUtil {
    public static Resource createTab(ResourceResolver resolver, String path, String title, Collection<Resource> children) {

        Resource items = createResource(resolver, path + "/items", Collections.emptyMap(), children);

        Map<String, Object> properties = new HashMap<>();
        properties.put(JcrConstants.JCR_TITLE, title);
        properties.put(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, "granite/ui/components/coral/foundation/container");
        properties.put("granite:class", "foundation-layout-util-vmargin");
        return createResource(resolver, path, properties, Collections.singletonList(items));
    }

    public static Resource createResource(ResourceResolver resolver, String path, Map<String, Object> properties, Collection<Resource> children) {

        ValueMap valueMap = new ValueMapDecorator(properties);
        String resourceType = StringUtils.defaultIfEmpty(properties.getOrDefault(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, StringUtils.EMPTY).toString(), JcrConstants.NT_UNSTRUCTURED);
        return new ValueMapResource(resolver, path, resourceType, valueMap, children);
    }
}
