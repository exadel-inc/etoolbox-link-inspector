package com.exadel.etoolbox.linkinspector.api.service;

import com.exadel.etoolbox.linkinspector.api.entity.LinkStatus;

public interface LinkTypeProvider {
    /**
     * @return Link type Name
     */
    String getName();

    /**
     * Obtains link value from given property
     * @param propertyValue
     * @return link value, null when property value is not related to the link type
     */
    String getLinkValue(String propertyValue);

    /**
     * Validates link from given link
     * @param link
     * @return LinkStatus object
     */
    LinkStatus validate(String link);
}
