package com.exadel.aembox.linkchecker.core.services.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Optional;

public class JsonUtil {
    private JsonUtil() {}

    private static final Logger LOG = LoggerFactory.getLogger(JsonUtil.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static JSONArray objectsToJsonArray(Collection<?> objects) {
        JSONArray jsonArray = new JSONArray();
        objects.forEach(object -> {
            try {
                String json = OBJECT_MAPPER.writeValueAsString(object);
                jsonArray.put(new JSONObject(json));
            } catch (JsonProcessingException | JSONException e) {
                LOG.error("Failed to convert gridResources to JSON", e);
            }
        });
        return jsonArray;
    }

    public static <T> T jsonToModel(JSONObject json, Class<T> modelClass) {
        try (InputStream is = new ByteArrayInputStream(json.toString().getBytes())) {
            final JavaType type = OBJECT_MAPPER.getTypeFactory().constructType(modelClass);
            return OBJECT_MAPPER.readValue(is, type);
        } catch (IOException e) {
            LOG.error("Failed to map json to model", e);
        }
        return null;
    }

    public static JSONArray getJsonArrayFromFile(String jsonPath, ResourceResolver resourceResolver) {
        Optional<InputStream> streamOptional = Optional.ofNullable(resourceResolver.getResource(jsonPath))
                .map(resource -> resource.adaptTo(InputStream.class));
        if (streamOptional.isPresent()) {
            try {
                String stringValue = IOUtils.toString(streamOptional.get(), StandardCharsets.UTF_8);
                return new JSONArray(stringValue);
            } catch (IOException e) {
                LOG.error("Failed to convert file stream to string", e);
            } catch (JSONException e) {
                LOG.error("Failed to get json array", e);
            }
        } else {
            LOG.debug("Failed to get json array from {}", jsonPath);
        }
        return new JSONArray();
    }
}