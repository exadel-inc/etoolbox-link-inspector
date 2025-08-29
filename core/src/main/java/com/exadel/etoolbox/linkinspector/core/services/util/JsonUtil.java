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

/**
 * Utility class providing helper methods for JSON manipulation and conversion.
 * <p>
 * This class contains static utility methods for common JSON operations in the link inspector
 * context, such as converting objects to JSON, parsing JSON data into model objects, and
 * reading JSON files from the JCR repository.
 * <p>
 * <p><u>Note</u>: This class is not a part of the public API and is subject to change. Do not use it in your own code</p>
 */
public class JsonUtil {
    private static final Logger LOG = LoggerFactory.getLogger(JsonUtil.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonUtil() {}

    /**
     * Converts a collection of objects to a JSON array.
     *
     * @param objects The collection of objects to convert
     * @return An ArrayNode containing the JSON representation of all objects
     */
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

    /**
     * Converts a JSON node to a typed model object.
     *
     * @param <T> The target model type
     * @param json The JSON node to convert
     * @param modelClass The class of the model to create
     * @return An instance of the model class populated with data from the JSON, or null if conversion fails
     */
    public static <T> T jsonToModel(JsonNode json, Class<T> modelClass) {
        try (InputStream is = new ByteArrayInputStream(json.toString().getBytes(StandardCharsets.UTF_8))) {
            final JavaType type = OBJECT_MAPPER.getTypeFactory().constructType(modelClass);
            return OBJECT_MAPPER.readValue(is, type);
        } catch (IOException e) {
            LOG.error("Failed to map json to model", e);
        }
        return null;
    }

    /**
     * Reads a JSON array from a file in the JCR repository.
     *
     * @param jsonPath The repository path to the JSON file
     * @param resourceResolver ResourceResolver used to access the file
     * @return An ArrayNode containing the parsed JSON array, or an empty array if reading fails
     */
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