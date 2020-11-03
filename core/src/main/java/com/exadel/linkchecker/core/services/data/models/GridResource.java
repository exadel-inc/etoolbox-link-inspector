package com.exadel.linkchecker.core.services.data.models;

import com.exadel.linkchecker.core.models.Link;
import com.exadel.linkchecker.core.models.LinkStatus;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.Optional;

public class GridResource {
    @JsonIgnore
    private Link link;

    private final String resourcePath;
    private final String propertyName;
    private final String resourceType;

    public GridResource(String resourcePath, String propertyName, String resourceType) {
        this.resourcePath = resourcePath;
        this.propertyName = propertyName;
        this.resourceType = resourceType;
    }

    @JsonCreator
    public GridResource(@JsonProperty("propertyName") String propertyName,
                        @JsonProperty("resourcePath") String resourcePath,
                        @JsonProperty("href") String href,
                        @JsonProperty("type") String type,
                        @JsonProperty("statusMessage") String statusMessage,
                        @JsonProperty("resourceType") String resourceType,
                        @JsonProperty("statusCode") String statusCode) {
        this.resourcePath = resourcePath;
        this.propertyName = propertyName;
        this.resourceType = resourceType;

        Link link = new Link(href, Link.Type.valueOf(type.toUpperCase()));
        link.setStatus(new LinkStatus(Integer.parseInt(statusCode), statusMessage));
        this.link = link;
    }

    public Link getLink() {
        return link;
    }

    public void setLink(Link link) {
        this.link = link;
    }

    public String getHref() {
        return Optional.ofNullable(getLink())
                .map(Link::getHref)
                .orElse(StringUtils.EMPTY);
    }

    public String getType() {
        return Optional.ofNullable(getLink())
                .map(Link::getType)
                .map(Link.Type::getValue)
                .orElse(Link.Type.INTERNAL.getValue());
    }

    public int getStatusCode() {
        return Optional.ofNullable(getLink())
                .map(Link::getStatusCode)
                .orElse(HttpStatus.SC_NOT_FOUND);
    }

    public String getStatusMessage() {
        return Optional.ofNullable(getLink())
                .map(Link::getStatusMessage)
                .orElse(HttpStatus.getStatusText(HttpStatus.SC_NOT_FOUND));
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getResourceType() {
        return resourceType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GridResource that = (GridResource) o;
        return Objects.equals(link, that.link) &&
                resourcePath.equals(that.resourcePath) &&
                propertyName.equals(that.propertyName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(link, resourcePath, propertyName);
    }
}