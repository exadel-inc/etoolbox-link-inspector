package com.exadel.aembox.linkchecker.core.servlets;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.json.Json;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(AemContextExtension.class)
class ResourceExistCheckServletTest {
    private static final String PATH_REQUEST_PARAM = "path";
    private static final String TEST_PATH = "/content/test";
    private static final String RESOURCE_EXISTS_RESP_PARAM = "isResourceExist";

    private final AemContext context = new AemContext();

    private final ResourceExistCheckServlet fixture = new ResourceExistCheckServlet();

    private MockSlingHttpServletRequest request;
    private MockSlingHttpServletResponse response;

    @BeforeEach
    void setup() {
        request = context.request();
        response = context.response();
    }

    @Test
    void testResourceExists() {
        context.create().resource(TEST_PATH);
        request.addRequestParameter(PATH_REQUEST_PARAM, TEST_PATH);

        fixture.doPost(request, response);

        String expectedJsonResponse = buildExpectedJsonResponse(true);
        String jsonResponse = response.getOutputAsString();

        assertEquals(expectedJsonResponse, jsonResponse);
    }

    @Test
    void testResourceNotExist() {
        request.addRequestParameter(PATH_REQUEST_PARAM, TEST_PATH);

        fixture.doPost(request, response);

        String expectedJsonResponse = buildExpectedJsonResponse(false);
        String jsonResponse = response.getOutputAsString();

        assertEquals(expectedJsonResponse, jsonResponse);
    }

    @Test
    void testEmptyParams() {
        fixture.doPost(request, response);

        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
    }

    private String buildExpectedJsonResponse(boolean expected) {
        return Json.createObjectBuilder()
                .add(RESOURCE_EXISTS_RESP_PARAM, expected)
                .build()
                .toString();
    }
}