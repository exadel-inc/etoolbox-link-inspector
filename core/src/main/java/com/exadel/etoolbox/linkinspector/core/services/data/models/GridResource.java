/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exadel.etoolbox.linkinspector.core.services.data.models;

import com.exadel.etoolbox.linkinspector.api.Link;
import com.exadel.etoolbox.linkinspector.api.LinkStatus;
import com.exadel.etoolbox.linkinspector.core.models.LinkImpl;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.Optional;

/**
 * Data model used for building data feed and further adaptation to sling resources for rendering
 * the Link Inspector grid. Represents data for a single row in the grid.
 */
public class GridResource {
    /**
     * JCR property names
     */
    public static final String PN_LINK = "link";
    public static final String PN_LINK_TYPE = "linkType";
    public static final String PN_LINK_STATUS_CODE = "linkStatusCode";
    public static final String PN_LINK_STATUS_MESSAGE = "linkStatusMessage";
    public static final String PN_RESOURCE_PATH = "resourcePath";
    public static final String PN_PROPERTY_NAME = "propertyName";

    /**
     * JSON field names
     */
    private static final String JSON_PROPERTY_NAME = PN_PROPERTY_NAME;
    private static final String JSON_RESOURCE_PATH = PN_RESOURCE_PATH;
    private static final String JSON_HREF = "href";
    private static final String JSON_TYPE = "type";
    private static final String JSON_STATUS_MESSAGE = "statusMessage";
    private static final String JSON_RESOURCE_TYPE = "resourceType";
    private static final String JSON_STATUS_CODE = "statusCode";

    @JsonIgnore
    private Link link;

    private final String resourcePath;
    private final String propertyName;
    private final String resourceType;

    public GridResource(Link link, String resourcePath, String propertyName, String resourceType) {
        this.link = link;
        this.resourcePath = resourcePath;
        this.propertyName = propertyName;
        this.resourceType = resourceType;
    }

    /**
     * Creates an instance of grid resource based on the data feed JSON fields
     *
     * @param propertyName  - data feed JSON field
     * @param resourcePath  - data feed JSON field
     * @param href          - data feed JSON field
     * @param type          - data feed JSON field
     * @param statusMessage - data feed JSON field
     * @param resourceType  - data feed JSON field
     * @param statusCode    - data feed JSON field
     */
    @JsonCreator
    public GridResource(@JsonProperty(JSON_PROPERTY_NAME) String propertyName,
                        @JsonProperty(JSON_RESOURCE_PATH) String resourcePath,
                        @JsonProperty(JSON_HREF) String href,
                        @JsonProperty(JSON_TYPE) String type,
                        @JsonProperty(JSON_STATUS_MESSAGE) String statusMessage,
                        @JsonProperty(JSON_RESOURCE_TYPE) String resourceType,
                        @JsonProperty(JSON_STATUS_CODE) String statusCode) {
        this.resourcePath = resourcePath;
        this.propertyName = propertyName;
        this.resourceType = resourceType;
        this.link = new LinkImpl(type, href, new LinkStatus(Integer.parseInt(statusCode), statusMessage));
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

    public String getMatchedText() {
        return Optional.ofNullable(getLink())
                .map(Link::getMatchedText)
                .orElse(StringUtils.EMPTY);
    }

    public String getType() {
        return Optional.ofNullable(getLink())
                .map(Link::getType)
                .orElse("internal");
    }

    public int getStatusCode() {
        return Optional.ofNullable(getLink())
                .map(Link::getStatus)
                .map(LinkStatus::getCode)
                .orElse(HttpStatus.SC_NOT_FOUND);
    }

    public String getStatusMessage() {
        return Optional.ofNullable(getLink())
                .map(Link::getStatus)
                .map(LinkStatus::getMessage)
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