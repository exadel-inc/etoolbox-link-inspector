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

package com.exadel.etoolbox.linkinspector.core.servlets;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.commons.httpclient.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.json.Json;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(AemContextExtension.class)
class ResourceExistCheckServletTest {
    private static final String PATH_REQUEST_PARAM = "path";
    private static final String TEST_PATH = "/content/test";
    private static final String RESOURCE_EXISTS_RESP_PARAM = "resourceExists";

    private final AemContext context = new AemContext();

    private ResourceExistCheckServlet fixture;

    @BeforeEach
    void setup() {
        fixture = context.registerInjectActivateService(new ResourceExistCheckServlet());
    }

    @Test
    void testResourceExists() {
        context.create().resource(TEST_PATH);
        context.request().addRequestParameter(PATH_REQUEST_PARAM, TEST_PATH);

        fixture.doPost(context.request(), context.response());

        String expectedJsonResponse = buildExpectedJsonResponse(true);
        String jsonResponse = context.response().getOutputAsString();

        assertEquals(expectedJsonResponse, jsonResponse);
    }

    @Test
    void testResourceNotExist() {
        context.request().addRequestParameter(PATH_REQUEST_PARAM, TEST_PATH);

        fixture.doPost(context.request(), context.response());

        String expectedJsonResponse = buildExpectedJsonResponse(false);
        String jsonResponse = context.response().getOutputAsString();

        assertEquals(expectedJsonResponse, jsonResponse);
    }

    @Test
    void testEmptyParams() {
        fixture.doPost(context.request(), context.response());

        assertEquals(HttpStatus.SC_BAD_REQUEST, context.response().getStatus());
    }

    private String buildExpectedJsonResponse(boolean expected) {
        return Json.createObjectBuilder()
                .add(RESOURCE_EXISTS_RESP_PARAM, expected)
                .build()
                .toString();
    }
}