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

package com.exadel.etoolbox.linkinspector.core.services.util;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Optional;

public class JsonUtil {
    private static final Logger LOG = LoggerFactory.getLogger(JsonUtil.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonUtil() {}

    public static ArrayNode objectsToJsonArray(Collection<?> objects) {
        ArrayNode arrayNode = OBJECT_MAPPER.createArrayNode();
        objects.forEach(object -> {
            try {
                JsonNode jsonNode = OBJECT_MAPPER.valueToTree(object);
                arrayNode.add(jsonNode);
            } catch (IllegalArgumentException e) {
                LOG.error("Failed to convert gridResources to JSON", e);
            }
        });
        return arrayNode;
    }

    public static <T> T jsonToModel(JsonNode json, Class<T> modelClass) {
        try (InputStream is = new ByteArrayInputStream(json.toString().getBytes(StandardCharsets.UTF_8))) {
            final JavaType type = OBJECT_MAPPER.getTypeFactory().constructType(modelClass);
            return OBJECT_MAPPER.readValue(is, type);
        } catch (IOException e) {
            LOG.error("Failed to map json to model", e);
        }
        return null;
    }

    public static ArrayNode getJsonArrayFromFile(String jsonPath, ResourceResolver resourceResolver) {
        Optional<InputStream> streamOptional = Optional.ofNullable(resourceResolver.getResource(jsonPath))
                .map(resource -> resource.adaptTo(InputStream.class));
        if (streamOptional.isPresent()) {
            try {
                String stringValue = IOUtils.toString(streamOptional.get(), StandardCharsets.UTF_8);
                return (ArrayNode) OBJECT_MAPPER.readTree(stringValue);
            } catch (IOException e) {
                LOG.error("Failed to get JSON array", e);
            }
        } else {
            LOG.debug("Failed to get json array from {}", jsonPath);
        }
        return OBJECT_MAPPER.createArrayNode();
    }
}