package com.exadel.etoolbox.linkinspector.core.services.data.impl;

import com.exadel.etoolbox.linkinspector.core.services.data.UserConfig;
import com.exadel.etoolbox.linkinspector.core.services.helpers.RepositoryHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component(service = UserConfig.class)
@Slf4j
public class UserConfigImpl implements UserConfig {

    @Reference
    private RepositoryHelper repositoryHelper;

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Annotation> T apply(T baseConfig, Class<?> clazz) {
        Handler<T> handler = new Handler<>(baseConfig, getProperties(clazz.getName()));
        Object proxy = Proxy.newProxyInstance(
                baseConfig.annotationType().getClassLoader(),
                new Class[]{baseConfig.annotationType()},
                handler);
        return (T) proxy;
    }

    private Map<String, Object> getProperties(String configId) {
        try (ResourceResolver resourceResolver = repositoryHelper.getServiceResourceResolver()) {
            Resource resource = resourceResolver.getResource(ConfigServiceImpl.CONFIG_PATH + "/" + configId);
            ValueMap valueMap = resource != null ? resource.getValueMap() : null;
            return valueMap != null ? new HashMap<>(valueMap) : Collections.emptyMap();
        }
    }

    @RequiredArgsConstructor
    private static class Handler<T extends Annotation> implements InvocationHandler {
        private static final String METHOD_TO_STRING = "toString";
        private static final String METHOD_ANNOTATION_TYPE = "annotationType";

        private final T source;
        private final Map<String, Object> overriddenProperties;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (METHOD_TO_STRING.equals(method.getName())) {
                return source.toString();
            }
            if (METHOD_ANNOTATION_TYPE.equals(method.getName())) {
                return source.annotationType();
            }
            Object override = MapUtils.getObject(overriddenProperties, method.getName());
            if (override != null) {
                Class<?> targetType = ClassUtils.primitiveToWrapper(method.getReturnType());
                if (targetType.equals(Integer.class)) {
                    return getInt(override);
                } else if (targetType.equals(Long.class)) {
                    return (long) getInt(override);
                } else if (targetType.equals(Boolean.class)) {
                    return getBoolean(override);
                } else {
                    return override.toString();
                }
            }
            try {
                return source.annotationType().getDeclaredMethod(method.getName()).invoke(source, args);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                log.error("Error retrieving property {}#{}", source.annotationType().getName(), method.getName(), e);
                return null;
            }
        }

        private static int getInt(Object value) {
            if (isBlank(value)) {
                return 0;
            }
            if (NumberUtils.isCreatable(value.toString())) {
                return Integer.parseInt(value.toString());
            }
            return 0;
        }

        private static boolean getBoolean(Object value) {
            if (isBlank(value)) {
                return false;
            }
            return !StringUtils.equalsAnyIgnoreCase(value.toString(), "false", "off");
        }

        private static boolean isBlank(Object value) {
            if (value == null) {
                return true;
            }
            return StringUtils.isBlank(value.toString());
        }
    }
}
