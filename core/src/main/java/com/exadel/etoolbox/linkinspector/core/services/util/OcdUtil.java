package com.exadel.etoolbox.linkinspector.core.services.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OcdUtil {

    private static final String SEPARATOR_COLON = ":";
    private static final String SEPARATOR_DASH = "-";

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
        ObjectClassDefinition objectClassDefinition = metaTypeInformation.getObjectClassDefinition(
                component.getClass().getName(),
                null);
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
