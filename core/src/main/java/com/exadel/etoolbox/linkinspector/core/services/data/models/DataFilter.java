package com.exadel.etoolbox.linkinspector.core.services.data.models;

import org.apache.commons.lang3.StringUtils;

/**
 * Filter model which is used to obtain the final list of data to display.
 * This class provides filtering capabilities for GridResource objects based on
 * link type and substring matching.
 */
public class DataFilter {

    private String type;
    private String substring;

    public DataFilter() {}

    public DataFilter(String type, String substring) {
        this.type = type;
        this.substring = substring;
    }

    /**
     * Validates if a GridResource passes the filter criteria
     *
     * @param gridResource The resource to validate against the filter
     * @return true if the resource matches the filter criteria, false otherwise
     */
    public boolean validate(GridResource gridResource) {
        return (StringUtils.isBlank(type) || StringUtils.equalsIgnoreCase(gridResource.getType(), this.type))
                && (StringUtils.isBlank(substring) || StringUtils.contains(gridResource.getValue(), substring));
    }
}
