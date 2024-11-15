package com.exadel.etoolbox.linkinspector.core.services.data.impl;

import com.exadel.etoolbox.linkinspector.core.services.data.UserConfig;
import com.exadel.etoolbox.linkinspector.core.services.helpers.RepositoryHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
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
        try(ResourceResolver resourceResolver = repositoryHelper.getServiceResourceResolver()){
            Resource resource = resourceResolver.getResource(ConfigServiceImpl.CONFIG_PATH + "/" + configId);
            if (resource == null) {
                return Collections.emptyMap();
            }
            Map<String, Object> properties = new HashMap<>();
            properties.putAll(resource.adaptTo(ValueMap.class));
            return properties;
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
            if (isNotBlank(override)) {
                if (NumberUtils.isCreatable(override.toString())) {
                    return Integer.parseInt(override.toString());
                } else if (StringUtils.equalsAnyIgnoreCase(override.toString(), "true", "false")) {
                    return Boolean.parseBoolean(override.toString());
                }
                return override;
            }
            try {
                return source.annotationType().getDeclaredMethod(method.getName()).invoke(source, args);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                log.error("Error retrieving property {}#{}", source.annotationType().getName(), method.getName(), e);
                return null;
            }
        }

        private static boolean isNotBlank(Object value) {
            if (value == null) {
                return false;
            }
            return StringUtils.isNotBlank(value.toString());
        }
    }
}
