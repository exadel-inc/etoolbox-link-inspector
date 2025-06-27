/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

/**
 * Utility class providing helper methods for working with AEM Granite UI components.
 * <p>
 * This class offers utility methods for creating synthetic resources that represent
 * Granite UI components such as tabs and containers. These resources are created in memory
 * and can be used in the construction of dynamic UI components for the Link Inspector
 * interface.
 * <p>
 * <p><u>Note</u>: This class is not a part of the public API and is subject to change. Do not use it in your own code</p>
 */
public class GraniteUtil {

    /**
     * Creates a tab resource with the specified properties and child resources.
     * <p>
     * This method creates a synthetic resource representing a Granite UI tab with
     * the provided title. It automatically creates an "items" child resource to
     * contain the tab's content.
     *
     * @param resolver The ResourceResolver to associate with the created resources
     * @param path The path for the tab resource
     * @param title The display title for the tab
     * @param children Collection of child resources to place within the tab's items container
     * @return A synthetic Resource representing a Granite UI tab
     */
    public static Resource createTab(ResourceResolver resolver, String path, String title, Collection<Resource> children) {

        Resource items = createResource(resolver, path + "/items", Collections.emptyMap(), children);

        Map<String, Object> properties = new HashMap<>();
        properties.put(JcrConstants.JCR_TITLE, title);
        properties.put(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, "granite/ui/components/coral/foundation/container");
        properties.put("granite:class", "foundation-layout-util-vmargin");
        return createResource(resolver, path, properties, Collections.singletonList(items));
    }

    /**
     * Creates a synthetic resource with the specified path, properties, and children.
     * <p>
     * This method is a general-purpose utility for creating in-memory resources that
     * can be used in the construction of Granite UI components. If no resource type
     * is specified in the properties, it defaults to "nt:unstructured".
     *
     * @param resolver The ResourceResolver to associate with the created resource
     * @param path The path for the resource
     * @param properties Map of properties to set on the resource
     * @param children Collection of child resources to add to the created resource
     * @return A synthetic ValueMapResource with the specified properties and children
     */
    public static Resource createResource(ResourceResolver resolver, String path, Map<String, Object> properties, Collection<Resource> children) {

        ValueMap valueMap = new ValueMapDecorator(properties);
        String resourceType = StringUtils.defaultIfEmpty(properties.getOrDefault(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, StringUtils.EMPTY).toString(), JcrConstants.NT_UNSTRUCTURED);
        return new ValueMapResource(resolver, path, resourceType, valueMap, children);
    }
}
