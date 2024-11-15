package com.exadel.etoolbox.linkinspector.core.services.data;

import java.lang.annotation.Annotation;

public interface UserConfig {

    <T extends Annotation> T apply(T baseConfig, Class<?> clazz);
}