package com.exadel.linkchecker.core.services.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;

import java.util.Optional;

public class ServletUtil {
    private ServletUtil() {}

    public static String getRequestParamString(SlingHttpServletRequest request, String param) {
        return Optional.ofNullable(request.getRequestParameter(param))
                .map(RequestParameter::getString)
                .orElse(StringUtils.EMPTY);
    }

    public static boolean getRequestParamBoolean(SlingHttpServletRequest request, String param) {
        return Boolean.parseBoolean(getRequestParamString(request, param));
    }
}