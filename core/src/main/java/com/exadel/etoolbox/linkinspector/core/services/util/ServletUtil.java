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

import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class providing methods for servlet request and response handling.
 * Offers helpers for parameter extraction, response writing, and other common
 * servlet-related operations used throughout the Link Inspector tool.
 */
public class ServletUtil {
    private static final Logger LOG = LoggerFactory.getLogger(ServletUtil.class);

    private ServletUtil() {}

    /**
     * Retrieves a request parameter as a String.
     *
     * @param request the SlingHttpServletRequest object
     * @param param   the name of the request parameter
     * @return the request parameter value as a String, or an empty string if not present
     */
    public static String getRequestParamString(SlingHttpServletRequest request, String param) {
        return Optional.ofNullable(request.getRequestParameter(param))
                .map(RequestParameter::getString)
                .orElse(StringUtils.EMPTY);
    }

    /**
     * Retrieves a request parameter as a List of Strings.
     *
     * @param request the SlingHttpServletRequest object
     * @param param   the name of the request parameter
     * @return the request parameter values as a List of Strings, or an empty list if not present
     */
    public static List<String> getRequestParamStringList(SlingHttpServletRequest request, String param) {
        return Optional.ofNullable(request.getRequestParameters(param))
                .map(requestParameters -> Arrays.stream(requestParameters)
                .map(RequestParameter::getString)
                .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    /**
     * Retrieves a request parameter as a boolean.
     *
     * @param request the SlingHttpServletRequest object
     * @param param   the name of the request parameter
     * @return the request parameter value as a boolean, or false if not present or not a valid boolean value
     */
    public static boolean getRequestParamBoolean(SlingHttpServletRequest request, String param) {
        return Boolean.parseBoolean(getRequestParamString(request, param));
    }

    /**
     * Retrieves a request parameter as an int.
     *
     * @param request      the SlingHttpServletRequest object
     * @param requestParam the name of the request parameter
     * @return the request parameter value as an int, or 0 if not present or not a valid integer value
     */
    public static int getRequestParamInt(SlingHttpServletRequest request, String requestParam) {
        String param = getRequestParamString(request, requestParam);
        return NumberUtils.isNumber(param) ? Integer.parseInt(param) : 0;
    }

    /**
     * Writes a JSON response.
     *
     * @param response the SlingHttpServletResponse object
     * @param json     the JSON string to write to the response
     */
    public static void writeJsonResponse(SlingHttpServletResponse response, String json) {
        response.setCharacterEncoding(CharEncoding.UTF_8);
        response.setContentType(ContentType.APPLICATION_JSON.getMimeType());
        try {
            response.getWriter().write(json);
        } catch (IOException e) {
            LOG.error("Failed to write json to response", e);
        }
    }
}