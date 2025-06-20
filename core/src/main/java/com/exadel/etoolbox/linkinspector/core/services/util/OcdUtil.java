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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * Utility class providing helper methods for working with OSGi Object Class Definitions (OCDs).
 * <p>
 * This class offers utilities for retrieving and formatting component labels from OSGi metadata.
 * It uses the OSGi MetaType service to access component definitions and their display names,
 * falling back to formatted class names when metadata is not available.
 * <p>
 * All methods are static and the class cannot be instantiated.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class OcdUtil {

    private static final String SEPARATOR_COLON = ":";
    private static final String SEPARATOR_DASH = "-";

    /**
     * Gets a human-readable label for an OSGi component.
     * <p>
     * This method attempts to retrieve the user-friendly name of the component from
     * its OSGi metadata using the MetaTypeService. If metadata is not available or
     * doesn't contain a name, it falls back to using the class name with camel case
     * formatting and "Impl" suffix removal.
     * <p>
     * If a label is found in the metadata but has a prefix (e.g., "namespace:label"
     * or "namespace-label"), the prefix is removed.
     *
     * @param component The OSGi component instance to get the label for
     * @param metaTypeService The MetaTypeService used to access component metadata
     * @return A user-friendly label for the component, or empty string if component is null
     */
    public static String getLabel(Object component, MetaTypeService metaTypeService) {
        if (component == null) {
            return StringUtils.EMPTY;
        }
        String result = splitCamelCase(component.getClass().getSimpleName().replace("Impl", StringUtils.EMPTY));
        if (metaTypeService == null) {
            return result;
        }
        Bundle bundle = FrameworkUtil.getBundle(component.getClass());
        MetaTypeInformation metaTypeInformation = metaTypeService.getMetaTypeInformation(bundle);
        if (metaTypeInformation == null) {
            return result;
        }
        ObjectClassDefinition objectClassDefinition = null;
        try {
            objectClassDefinition = metaTypeInformation.getObjectClassDefinition(
                    component.getClass().getName(),
                    null);
        } catch (IllegalArgumentException e) {
            log.error("Failed to get ObjectClassDefinition for {}.", component.getClass().getName());
        }
        if (objectClassDefinition == null || StringUtils.isBlank(objectClassDefinition.getName())) {
            return result;
        }
        return removePrefix(objectClassDefinition.getName());
    }

    private static String splitCamelCase(String value) {
        return value.replaceAll("([a-z])([A-Z]+)", "$1 $2");
    }

    private static String removePrefix(String value) {
        if (value.contains(SEPARATOR_COLON)) {
            return StringUtils.substringAfter(value, SEPARATOR_COLON).trim();
        }
        if (value.contains(SEPARATOR_DASH)) {
            return StringUtils.substringAfter(value, SEPARATOR_DASH).trim();
        }
        return value.trim();
    }
}
