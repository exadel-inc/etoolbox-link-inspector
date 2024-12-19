package com.exadel.etoolbox.linkinspector.core.models;

import com.exadel.etoolbox.linkinspector.core.services.util.CsvUtil;

public class UpdatedItem {
    private final String currentLink;
    private final String updatedLink;
    private final String path;
    private final String propertyName;

    public UpdatedItem(String currentLink, String updatedLink, String path, String propertyName) {
        this.currentLink = currentLink;
        this.updatedLink = updatedLink;
        this.path = path;
        this.propertyName = propertyName;
    }

    public String getPropertyLocation() {
        return CsvUtil.buildLocation(path, propertyName);
    }

    public String getUpdatedLink() {
        return updatedLink;
    }

    public String getCurrentLink() {
        return currentLink;
    }
}
