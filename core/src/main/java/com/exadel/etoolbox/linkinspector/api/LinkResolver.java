package com.exadel.etoolbox.linkinspector.api;

import org.apache.sling.api.resource.ResourceResolver;

import java.util.Collection;

public interface LinkResolver {
    /**
     * Retrieves the token used to identify the current link type
     * @return A non-blank string value
     */
    String getId();

    /**
     * Obtains link value(s) from the given string source
     * @param source A string value. A non-blank string is expected
     * @return A collection of {@link Link} objects. Can be an empty collection, but never {@code null}
     */
    Collection<Link> getLinks(String source);

    /**
     * Validates the provided {@link Link}
     * @param link A @code Link} object; a non-null reference is expected
     * @param resourceResolver {@link ResourceResolver} object; a non-null reference is expected
     */
    void validate(Link link, ResourceResolver resourceResolver);
}
