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

import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.day.cq.commons.jcr.JcrConstants;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ServletUtil {
    private static final Logger LOG = LoggerFactory.getLogger(ServletUtil.class);

    private ServletUtil() {}

    public static String getRequestParamString(SlingHttpServletRequest request, String param) {
        return Optional.ofNullable(request.getRequestParameter(param))
                .map(RequestParameter::getString)
                .orElse(StringUtils.EMPTY);
    }

    public static List<String> getRequestParamStringList(SlingHttpServletRequest request, String param) {
        return Optional.ofNullable(request.getRequestParameters(param))
                .map(requestParameters -> Arrays.stream(requestParameters)
                .map(RequestParameter::getString)
                .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    public static boolean getRequestParamBoolean(SlingHttpServletRequest request, String param) {
        return Boolean.parseBoolean(getRequestParamString(request, param));
    }

    public static int getRequestParamInt(SlingHttpServletRequest request, String requestParam) {
        String param = getRequestParamString(request, requestParam);
        return NumberUtils.isNumber(param) ? Integer.parseInt(param) : 0;
    }

    public static void writeJsonResponse(SlingHttpServletResponse response, String json) {
        response.setCharacterEncoding(CharEncoding.UTF_8);
        response.setContentType(ContentType.APPLICATION_JSON.getMimeType());
        try {
            response.getWriter().write(json);
        } catch (IOException e) {
            LOG.error("Failed to write json to response", e);
        }
    }

    public static Resource createTab(ResourceResolver resolver, String path, String title, Collection<Resource> children) {

        Resource items = createResource(resolver, path + "/items", Collections.emptyMap(), children);

        Map<String, Object> properties = new HashMap<>();
        properties.put(JcrConstants.JCR_TITLE, title);
        properties.put(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, "granite/ui/components/coral/foundation/container");
        return createResource(resolver, path, properties, Collections.singletonList(items));
    }

    public static Resource createResource(ResourceResolver resolver, String path, Map<String, Object> properties, Collection<Resource> children) {

        ValueMap valueMap = new ValueMapDecorator(properties);
        String resourceType = StringUtils.defaultIfEmpty(properties.getOrDefault(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, StringUtils.EMPTY).toString(), JcrConstants.NT_UNSTRUCTURED);
        return new ValueMapResource(resolver, path, resourceType, valueMap, children);
    }
}