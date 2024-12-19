package com.exadel.etoolbox.linkinspector.core.services.data.models;

import com.exadel.etoolbox.linkinspector.core.services.util.CsvUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UpdatedItem {

    @Getter
    private final String currentLink;

    @Getter
    private final String updatedLink;

    private final String path;
    private final String propertyName;

    public String getPropertyLocation() {
        return CsvUtil.buildLocation(path, propertyName);
    }

}
