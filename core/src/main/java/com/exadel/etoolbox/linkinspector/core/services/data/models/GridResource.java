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

import com.exadel.etoolbox.linkinspector.api.Result;
import com.exadel.etoolbox.linkinspector.api.Status;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Data model used for building data feed and further adaptation to sling resources for rendering
 * the Link Inspector grid. Represents data for a single row in the grid.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Builder
@Getter
@EqualsAndHashCode
@JsonIgnoreProperties(value = "reported")
public class GridResource implements Result {

    private String resourcePath;

    private String propertyName;

    private String resourceType;

    private String type;

    private String value;

    private String match;

    @JsonIgnore
    @Getter(lazy = true)
    private final Status status = prepareStatus();

    private int statusCode;

    private String statusMessage;

    public GridResource(Result result, String resourcePath, String propertyName, String resourceType) {
        this.resourcePath = resourcePath;
        this.propertyName = propertyName;
        this.resourceType = resourceType;
        setResult(result);
    }

    public void setResult(Result result) {
        this.type = result.getType();
        this.value = result.getValue();
        this.match = result.getMatch();
        setStatus(result.getStatus());
    }

    @Override
    public void setStatus(Status status) {
        this.statusCode = status.getCode();
        this.statusMessage = status.getMessage();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("resourcePath", resourcePath);
        map.put("propertyName", propertyName);
        map.put("type", type);
        map.put("value", value);
        map.put("match", match);
        map.put("statusCode", statusCode);
        map.put("statusMessage", statusMessage);
        return map;
    }

    private Status prepareStatus() {
        return new Status(statusCode, statusMessage);
    }
}