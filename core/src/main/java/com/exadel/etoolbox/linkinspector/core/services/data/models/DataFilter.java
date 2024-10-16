package com.exadel.etoolbox.linkinspector.core.services.data.models;

import org.apache.commons.lang3.StringUtils;

/**
 * Filter model which is used to obtain the final list of data to display.
 */
public class DataFilter {

    private String type;
    private String substring;

    public DataFilter() {}

    public DataFilter(String type, String substring) {
        this.type = type;
        this.substring = substring;
    }

    public boolean validate(GridResource gridResource) {
        return (StringUtils.isBlank(type) || StringUtils.equalsIgnoreCase(gridResource.getLink().getType(), this.type))
                && (StringUtils.isBlank(substring) || gridResource.getLink().getHref().contains(substring));
    }
}
