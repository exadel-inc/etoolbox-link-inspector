package com.exadel.linkchecker.core.services.util;

import org.apache.commons.lang3.StringUtils;

public class CsvUtil {
    private CsvUtil() {}

    public static final String CSV_MIME_TYPE = "text/csv";

    public static final String SEMICOLON = ";";
    public static final String QUOTE = "\"";
    public static final String AT_SIGN = "@";

    public static String wrapIfContainsSemicolon(String value) {
        if (value.contains(SEMICOLON)) {
            return StringUtils.wrap(value, QUOTE);
        }
        return value;
    }

    public static String buildLocation(String path, String propertyName) {
        return path + AT_SIGN + propertyName;
    }
}