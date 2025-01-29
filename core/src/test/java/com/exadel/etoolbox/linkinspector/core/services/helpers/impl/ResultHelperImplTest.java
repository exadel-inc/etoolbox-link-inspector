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

package com.exadel.etoolbox.linkinspector.core.services.helpers.impl;

import com.exadel.etoolbox.linkinspector.api.Result;
import com.exadel.etoolbox.linkinspector.api.LinkResolver;
import com.exadel.etoolbox.linkinspector.api.Status;
import com.exadel.etoolbox.linkinspector.core.models.LinkResult;
import com.exadel.etoolbox.linkinspector.core.services.helpers.LinkHelper;
import com.exadel.etoolbox.linkinspector.core.services.mocks.MockHttpClientBuilderFactory;
import com.exadel.etoolbox.linkinspector.core.services.mocks.MockRepositoryHelper;
import com.exadel.etoolbox.linkinspector.core.services.resolvers.ExternalLinkResolverImpl;
import com.exadel.etoolbox.linkinspector.core.services.resolvers.InternalLinkResolverImpl;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(AemContextExtension.class)
class ResultHelperImplTest {

    private static final String VALID_INTERNAL = "/content/test";
    private static final String VALID_INTERNAL_EXTENSION = "/content/test.png";
    private static final String VALID_INTERNAL_SPACES = "/content/test with spaces";
    private static final String VALID_INTERNAL_SPACES_EXTENSION = "/content/test with spaces.png";
    private static final String INVALID_INTERNAL = "/test";
    private static final String VALID_INTERNAL_ENCODED = "/content/test%20link";
    private static final String VALID_INTERNAL_DECODED = "/content/test link";

    private static final String VALID_EXTERNAL = "https://www.google.com";
    private static final String INVALID_EXTERNAL = "htt://google.com";

    private static final String SINGLE_LINK_HTML_TAG_PLACEHOLDER = "Lorem ipsum dolor sit amet, consectetur adipisicing elit <a href=\"%s\"></a>, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua";
    private static final String INVALID_SYNTAX_MESSAGE = "Unsupported link type";

    private static final String RESOURCE_PATH = "/content/test-resource";
    private static final String PROPERTY_NAME = "testProperty";
    private static final String CURRENT_LINK = "/content/link-for-replacement";
    private static final String NEW_LINK = "/content/replacement-link";
    private static final String INTERNAL_LINKS_HOST_FIELD = "internalLinksHost";
    private static final String INTERNAL_LINKS_HOST_FIELD_VALUE = "https://example.com";

    private final AemContext context = new AemContext();

    private LinkHelper fixture;
    private LinkResolver internalLinkResolver;
    private CloseableHttpClient httpClient;

    @BeforeEach
    void setup() throws IOException {
        context.registerInjectActivateService(new MockRepositoryHelper(context.resourceResolver()));

        CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
        when(httpResponse.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_0, HttpStatus.SC_NOT_FOUND, "Not Found"));

        httpClient = mock(CloseableHttpClient.class);
        when(httpClient.execute(any())).thenReturn(httpResponse);
        context.registerInjectActivateService(
                new MockHttpClientBuilderFactory(),
                Collections.singletonMap(MockHttpClientBuilderFactory.PN_CLIENT, httpClient));
        context.registerInjectActivateService(new ExternalLinkResolverImpl());
        internalLinkResolver = context.registerInjectActivateService(new InternalLinkResolverImpl());

        fixture = context.registerInjectActivateService(new LinkHelperImpl());
    }

    @Test
    void shouldReturnValidForMultiValued() {
        List<String> validLinks = Arrays.asList(
                VALID_INTERNAL,
                VALID_INTERNAL_EXTENSION,
                VALID_INTERNAL_SPACES,
                VALID_INTERNAL_SPACES_EXTENSION,
                VALID_EXTERNAL
        );
        List<String> invalidLinks = Arrays.asList(INVALID_INTERNAL, INVALID_EXTERNAL);
        String[] allLinks = Stream.concat(validLinks.stream(), invalidLinks.stream())
                .toArray(String[]::new);

        List<String> detectedLinks = fixture.getLinkStream(allLinks)
                .map(Result::getValue)
                .collect(Collectors.toList());

        assertTrue(detectedLinks.stream().noneMatch(invalidLinks::contains));
        assertTrue(validLinks.containsAll(detectedLinks));
    }

    @Test
    void shouldReturnSingleValidInternal() {
        testGetSingleLink(VALID_INTERNAL);
        testGetSingleLinkFromText(String.format(SINGLE_LINK_HTML_TAG_PLACEHOLDER, VALID_INTERNAL), VALID_INTERNAL);
    }

    @Test
    void shouldReturnSingleValidExternal() {
        testGetSingleLink(VALID_EXTERNAL);
        testGetSingleLinkFromText(String.format(SINGLE_LINK_HTML_TAG_PLACEHOLDER, VALID_EXTERNAL), VALID_EXTERNAL);
    }

    @Test
    void shouldReturnSingleValidInternalWithExtension() {
        testGetSingleLink(VALID_INTERNAL_EXTENSION);
        testGetSingleLinkFromText(String.format(SINGLE_LINK_HTML_TAG_PLACEHOLDER, VALID_INTERNAL_EXTENSION), VALID_INTERNAL_EXTENSION);
    }

    @Test
    void shouldReturnSingleValidInternalWithSpaces() {
        testGetSingleLink(VALID_INTERNAL_SPACES);
        testGetSingleLinkFromText(String.format(SINGLE_LINK_HTML_TAG_PLACEHOLDER, VALID_INTERNAL_SPACES), VALID_INTERNAL_SPACES);
    }

    @Test
    void shouldReturnSingleValidInternalWithSpacesExtension() {
        testGetSingleLink(VALID_INTERNAL_SPACES_EXTENSION);
        testGetSingleLinkFromText(String.format(SINGLE_LINK_HTML_TAG_PLACEHOLDER, VALID_INTERNAL_SPACES_EXTENSION), VALID_INTERNAL_SPACES_EXTENSION);
    }

    @Test
    void shouldReturnEmptyForInvalidInternal() {
        Optional<Result> linkOptional = fixture.getLinkStream(INVALID_INTERNAL).findFirst();

        assertFalse(linkOptional.isPresent());
    }

    @Test
    void shouldReturnEmptyForInvalidExternal() {
        Optional<Result> linkOptional = fixture.getLinkStream(INVALID_EXTERNAL).findFirst();

        assertFalse(linkOptional.isPresent());
    }

    @Test
    void testValidateInternalLink_valid() {
        context.create().resource(VALID_INTERNAL);

        Status linkStatus = fixture.validateLink(VALID_INTERNAL, context.resourceResolver());

        testLinkStatus(HttpStatus.SC_OK, linkStatus);
    }

    @Test
    void testValidateInternalEncodedLink_valid() {
        context.create().resource(VALID_INTERNAL_DECODED);

        Status linkStatus = fixture.validateLink(VALID_INTERNAL_ENCODED, context.resourceResolver());

        testLinkStatus(HttpStatus.SC_OK, linkStatus);
    }

    @Test
    void testValidateInternalLink_invalid() {
        Status linkStatus = fixture.validateLink(VALID_INTERNAL, context.resourceResolver());

        testLinkStatus(HttpStatus.SC_NOT_FOUND, linkStatus);
    }

    @Test
    void testValidateInternalEncodedLink_decodeException() {
        try (MockedStatic<URLDecoder> urlDecoder = mockStatic(URLDecoder.class)) {
            urlDecoder.when(() ->
                    URLDecoder.decode(eq(VALID_INTERNAL_ENCODED), anyString())
            ).thenThrow(new UnsupportedEncodingException());

            Status linkStatus = fixture.validateLink(VALID_INTERNAL_ENCODED, context.resourceResolver());

            testLinkStatus(HttpStatus.SC_NOT_FOUND, linkStatus);
        }
    }

    @Test
    void testValidateExternalLink_socketTimeout() throws IOException {
        when(httpClient.execute(any())).thenThrow(new SocketTimeoutException());

        Status linkStatus = fixture.validateLink(VALID_EXTERNAL, context.resourceResolver());

        testLinkStatus(HttpStatus.SC_REQUEST_TIMEOUT, "Request Timeout", linkStatus);
    }

    @Test
    void testValidateExternalLink_badRequest() throws IOException {
        Exception e = new IOException();
        when(httpClient.execute(any())).thenThrow(new IOException());

        Status linkStatus = fixture.validateLink(VALID_EXTERNAL, context.resourceResolver());

        testLinkStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.toString(), linkStatus);
    }

    @Test
    void testValidateLink_validInternal() {
        context.create().resource(VALID_INTERNAL);

        Result resultForValidation = new LinkResult("Internal", VALID_INTERNAL);
        fixture.validateLink(resultForValidation, context.resourceResolver());

        testLinkStatus(HttpStatus.SC_OK, resultForValidation.getStatus());
    }

    @Test
    void testValidateLink_invalidInternal() {
        Result resultForValidation = new LinkResult("Internal", VALID_INTERNAL);
        fixture.validateLink(resultForValidation, context.resourceResolver());

        testLinkStatus(HttpStatus.SC_NOT_FOUND, resultForValidation.getStatus());
    }

    @Test
    void testValidateLink_shouldSendRequestForInternal404LinksIfInternalLinksHostIsConfigured() throws IOException {
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_0, HttpStatus.SC_OK, "OK"));
        when(httpClient.execute(any())).thenReturn(response);

        MockOsgi.deactivate(internalLinkResolver, context.bundleContext());
        MockOsgi.activate(
                internalLinkResolver,
                context.bundleContext(),
                Collections.singletonMap(INTERNAL_LINKS_HOST_FIELD, INTERNAL_LINKS_HOST_FIELD_VALUE));

        Result resultForValidation = new LinkResult("Internal", VALID_INTERNAL);
        fixture.validateLink(resultForValidation, context.resourceResolver());

        testLinkStatus(HttpStatus.SC_OK, resultForValidation.getStatus());
    }

    @Test
    void testValidateLink_validExternal() throws IOException {
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_0, HttpStatus.SC_OK, "OK"));
        when(httpClient.execute(any())).thenReturn(response);

        Status linkStatus = fixture.validateLink(VALID_EXTERNAL, context.resourceResolver());

        testLinkStatus(HttpStatus.SC_OK, linkStatus);
    }

    @Test
    void testValidateLink_invalidExternal() {
        Status linkStatus = fixture.validateLink(INVALID_EXTERNAL, context.resourceResolver());

        testLinkStatus(HttpStatus.SC_BAD_REQUEST, INVALID_SYNTAX_MESSAGE, linkStatus);
    }
    @Test
    void testValidateLinkString_invalidSyntax() {
        String linkInsideText = String.format(SINGLE_LINK_HTML_TAG_PLACEHOLDER, VALID_INTERNAL);

        Status linkStatus = fixture.validateLink(linkInsideText, context.resourceResolver());

        testLinkStatus(HttpStatus.SC_BAD_REQUEST, INVALID_SYNTAX_MESSAGE, linkStatus);
    }

    @Test
    void testReplaceLink_singleValue() {
        Resource resource = context.create().resource(RESOURCE_PATH, PROPERTY_NAME, CURRENT_LINK);

        fixture.replaceLink(context.resourceResolver(), RESOURCE_PATH, PROPERTY_NAME, CURRENT_LINK, NEW_LINK);

        String updatedValue = resource.getValueMap().get(PROPERTY_NAME, String.class);
        assertEquals(NEW_LINK, updatedValue);
    }

    @Test
    void testReplaceLink_multiValue() {
        String[] multiValue = {CURRENT_LINK, CURRENT_LINK};
        Resource resource = context.create().resource(RESOURCE_PATH, PROPERTY_NAME, multiValue);

        fixture.replaceLink(context.resourceResolver(), RESOURCE_PATH, PROPERTY_NAME, CURRENT_LINK, NEW_LINK);

        String[] updatedValue = resource.getValueMap().get(PROPERTY_NAME, String[].class);
        assertNotNull(updatedValue);
        assertTrue(Arrays.stream(updatedValue).allMatch(NEW_LINK::equals));
    }

    @Test
    void testReplaceLink_currentLinkNotFound() {
        Resource resource = context.create().resource(RESOURCE_PATH, PROPERTY_NAME, VALID_INTERNAL);

        fixture.replaceLink(context.resourceResolver(), RESOURCE_PATH, PROPERTY_NAME, CURRENT_LINK, NEW_LINK);

        String updatedValue = resource.getValueMap().get(PROPERTY_NAME, String.class);
        assertEquals(VALID_INTERNAL, updatedValue);
    }

    private void testLinkStatus(int expectedCode, Status linkStatus) {
        assertEquals(expectedCode, linkStatus.getCode());
    }

    private void testLinkStatus(int expectedCode, String expectedMessage, Status linkStatus) {
        assertEquals(expectedCode, linkStatus.getCode());
        assertEquals(expectedMessage, linkStatus.getMessage());
    }

    private void testGetSingleLink(String link) {
        testGetSingleLinkFromText(link, link);
    }

    private void testGetSingleLinkFromText(String text, String expected) {
        Optional<Result> linkOptional = fixture.getLinkStream(text).findFirst();

        assertTrue(linkOptional.isPresent());
        assertEquals(expected, linkOptional.get().getValue());
    }
}