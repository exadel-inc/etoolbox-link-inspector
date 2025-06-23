package com.exadel.etoolbox.linkinspector.core.services.data.models;

import com.exadel.etoolbox.linkinspector.core.services.util.CsvUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p><u>Note</u>: This class is not a part of the public API and is subject to change. Do not use it in your own code</p>
 * Represents an item that has been updated during a link fixing operation.
 * This class stores information about the link before and after updating,
 * as well as the location where the update was performed.
 */
@RequiredArgsConstructor
public class UpdatedItem {

    @Getter
    private final String currentLink;

    @Getter
    private final String updatedLink;

    private final String path;
    private final String propertyName;

    /**
     * Returns the property location in a formatted string combining path and property name
     *
     * @return A formatted string representing the property location
     */
    public String getPropertyLocation() {
        return CsvUtil.buildLocation(path, propertyName);
    }

}
