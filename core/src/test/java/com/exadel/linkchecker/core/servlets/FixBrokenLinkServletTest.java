package com.exadel.linkchecker.core.servlets;

import com.exadel.linkchecker.core.models.LinkStatus;
import com.exadel.linkchecker.core.services.helpers.LinkHelper;
import com.exadel.linkchecker.core.services.helpers.RepositoryHelper;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import junitx.util.PrivateAccessor;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.json.Json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(AemContextExtension.class)
class FixBrokenLinkServletTest {
    private static final String REPOSITORY_HELPER_FIELD = "repositoryHelper";
    private static final String LINK_HELPER_FIELD = "linkHelper";

    private static final String PATH_PARAM = "path";
    private static final String PROPERTY_NAME_PARAM = "propertyName";
    private static final String CURRENT_LINK_PARAM = "currentLink";
    private static final String NEW_LINK_PARAM = "newLink";
    private static final String IS_SKIP_VALIDATION_PARAM = "isSkipValidation";
    private static final String STATUS_CODE_RESP_PARAM = "statusCode";
    private static final String STATUS_MSG_RESP_PARAM = "statusMessage";

    private static final String TEST_PATH = "/content/test";
    private static final String TEST_PROPERTY_NAME = "testProperty";
    private static final String TEST_CURRENT_LINK = "/content/link-for-replacement";
    private static final String TEST_NEW_LINK = "/content/replacement-link";
    private static final String TEST_EXCEPTION_MSG = "Test exception message";

    private final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    private final FixBrokenLinkServlet fixture = new FixBrokenLinkServlet();

    private LinkHelper linkHelper;
    private RepositoryHelper repositoryHelper;
    private ResourceResolver resourceResolver;

    private MockSlingHttpServletRequest request;
    private MockSlingHttpServletResponse response;

    @BeforeEach
    void setup() throws NoSuchFieldException {
        linkHelper = mock(LinkHelper.class);
        repositoryHelper = mock(RepositoryHelper.class);
        resourceResolver = mock(ResourceResolver.class);
        PrivateAccessor.setField(fixture, LINK_HELPER_FIELD, linkHelper);
        PrivateAccessor.setField(fixture, REPOSITORY_HELPER_FIELD, repositoryHelper);

        request = context.request();
        response = context.response();
    }

    @Test
    void testEmptyParams() {
        fixture.doPost(request, response);

        assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());
        verifyNoInteractions(linkHelper);
        verifyNoInteractions(repositoryHelper);
    }

    @Test
    void testCurrentLinkEqualToReplacement() {
        request.addRequestParameter(PATH_PARAM, TEST_PATH);
        request.addRequestParameter(PROPERTY_NAME_PARAM, TEST_PROPERTY_NAME);
        request.addRequestParameter(CURRENT_LINK_PARAM, TEST_CURRENT_LINK);
        request.addRequestParameter(NEW_LINK_PARAM, TEST_CURRENT_LINK);
        fixture.doPost(request, response);

        assertEquals(HttpStatus.SC_ACCEPTED, response.getStatus());
        verifyNoInteractions(linkHelper);
        verifyNoInteractions(repositoryHelper);
    }

    @Test
    void testValidateNewLink_notValid() {
        LinkStatus expectedLinkStatus =
                new LinkStatus(HttpStatus.SC_NOT_FOUND, HttpStatus.getStatusText(HttpStatus.SC_NOT_FOUND));
        when(linkHelper.validateLink(eq(TEST_NEW_LINK), eq(resourceResolver))).thenReturn(expectedLinkStatus);
        when(repositoryHelper.getServiceResourceResolver()).thenReturn(resourceResolver);

        request.addRequestParameter(PATH_PARAM, TEST_PATH);
        request.addRequestParameter(PROPERTY_NAME_PARAM, TEST_PROPERTY_NAME);
        request.addRequestParameter(CURRENT_LINK_PARAM, TEST_CURRENT_LINK);
        request.addRequestParameter(NEW_LINK_PARAM, TEST_NEW_LINK);
        request.addRequestParameter(IS_SKIP_VALIDATION_PARAM, Boolean.FALSE.toString());
        fixture.doPost(request, response);

        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        verify(linkHelper, never())
                .replaceLink(any(ResourceResolver.class), eq(TEST_PATH), eq(TEST_PROPERTY_NAME), eq(TEST_CURRENT_LINK), eq(TEST_NEW_LINK));
        verify(repositoryHelper, never()).createResourceIfNotExist(anyString(), anyString(), anyString());

        String expectedJsonResponse = Json.createObjectBuilder()
                .add(STATUS_CODE_RESP_PARAM, expectedLinkStatus.getStatusCode())
                .add(STATUS_MSG_RESP_PARAM, expectedLinkStatus.getStatusMessage())
                .build()
                .toString();
        String jsonResponse = response.getOutputAsString();
        assertEquals(expectedJsonResponse, jsonResponse);
    }

    @Test
    void testReplacement_failed() {
        when(linkHelper.replaceLink(any(ResourceResolver.class), eq(TEST_PATH), eq(TEST_PROPERTY_NAME), eq(TEST_CURRENT_LINK), eq(TEST_NEW_LINK)))
                .thenReturn(false);
        request.addRequestParameter(PATH_PARAM, TEST_PATH);
        request.addRequestParameter(PROPERTY_NAME_PARAM, TEST_PROPERTY_NAME);
        request.addRequestParameter(CURRENT_LINK_PARAM, TEST_CURRENT_LINK);
        request.addRequestParameter(NEW_LINK_PARAM, TEST_NEW_LINK);
        request.addRequestParameter(IS_SKIP_VALIDATION_PARAM, Boolean.TRUE.toString());
        fixture.doPost(request, response);

        assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatus());
        verify(repositoryHelper, never()).createResourceIfNotExist(anyString(), anyString(), anyString());
    }

    @Test
    void testReplacement_success() {
        when(linkHelper.replaceLink(any(ResourceResolver.class), eq(TEST_PATH), eq(TEST_PROPERTY_NAME), eq(TEST_CURRENT_LINK), eq(TEST_NEW_LINK)))
                .thenReturn(true);
        request.addRequestParameter(PATH_PARAM, TEST_PATH);
        request.addRequestParameter(PROPERTY_NAME_PARAM, TEST_PROPERTY_NAME);
        request.addRequestParameter(CURRENT_LINK_PARAM, TEST_CURRENT_LINK);
        request.addRequestParameter(NEW_LINK_PARAM, TEST_NEW_LINK);
        request.addRequestParameter(IS_SKIP_VALIDATION_PARAM, Boolean.TRUE.toString());
        fixture.doPost(request, response);

        verify(repositoryHelper, atLeastOnce()).createResourceIfNotExist(anyString(), anyString(), anyString());
        assertEquals(HttpStatus.SC_OK, response.getStatus());
    }

    @Test
    void testReplacement_commitException() throws PersistenceException {
        SlingHttpServletRequest requestMock = mock(SlingHttpServletRequest.class);
        ResourceResolver resourceResolverMock = mock(ResourceResolver.class);

        mockRequestParam(PATH_PARAM, TEST_PATH, requestMock);
        mockRequestParam(PROPERTY_NAME_PARAM, TEST_PROPERTY_NAME, requestMock);
        mockRequestParam(CURRENT_LINK_PARAM, TEST_CURRENT_LINK, requestMock);
        mockRequestParam(NEW_LINK_PARAM, TEST_NEW_LINK, requestMock);
        mockRequestParam(IS_SKIP_VALIDATION_PARAM, Boolean.TRUE.toString(), requestMock);

        when(requestMock.getResourceResolver()).thenReturn(resourceResolverMock);
        when(linkHelper.replaceLink(eq(resourceResolverMock), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(true);
        doThrow(new PersistenceException(TEST_EXCEPTION_MSG)).when(resourceResolverMock).commit();

        fixture.doPost(requestMock, response);

        verify(repositoryHelper, atLeastOnce()).createResourceIfNotExist(anyString(), anyString(), anyString());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
    }

    private void mockRequestParam(String paramName, String value, SlingHttpServletRequest requestMock) {
        RequestParameter requestParameter = mock(RequestParameter.class);
        when(requestMock.getRequestParameter(paramName)).thenReturn(requestParameter);
        when(requestParameter.getString()).thenReturn(value);
    }
}