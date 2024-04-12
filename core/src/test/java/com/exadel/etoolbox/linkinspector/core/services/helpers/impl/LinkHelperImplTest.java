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

import com.exadel.etoolbox.linkinspector.core.models.Link;
import com.exadel.etoolbox.linkinspector.api.dto.LinkStatus;
import com.exadel.etoolbox.linkinspector.core.services.ExternalLinkChecker;
import com.exadel.etoolbox.linkinspector.core.services.ext.CustomLinkResolver;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import junitx.util.PrivateAccessor;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(AemContextExtension.class)
class LinkHelperImplTest {
    private static final String EXTERNAL_LINK_CHECKER_FIELD = "externalLinkChecker";
    private static final String CUSTOM_LINK_FIELD = "customLinkResolver";

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
    private static final String INVALID_SYNTAX_MESSAGE = "The provided link doesn't fit internal nor external link patterns";

    private static final String RESOURCE_PATH = "/content/test-resource";
    private static final String PROPERTY_NAME = "testProperty";
    private static final String CURRENT_LINK = "/content/link-for-replacement";
    private static final String NEW_LINK = "/content/replacement-link";
    private static final String INTERNAL_LINKS_HOST_FIELD = "internalLinksHost";
    private static final String INTERNAL_LINKS_HOST_FIELD_VALUE = "http://example.com";

    private final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    private final LinkHelperImpl linkHelper = new LinkHelperImpl();

    private ExternalLinkChecker externalLinkChecker;

    @BeforeEach
    void setup() throws NoSuchFieldException {
        externalLinkChecker = mock(ExternalLinkChecker.class);
        PrivateAccessor.setField(linkHelper, EXTERNAL_LINK_CHECKER_FIELD, externalLinkChecker);

        CustomLinkResolver customLinkResolver = mock(CustomLinkResolver.class);
        when(customLinkResolver.getLinks(anyString())).thenReturn(new ArrayList<>());
        PrivateAccessor.setField(linkHelper, CUSTOM_LINK_FIELD, customLinkResolver);
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

        List<String> detectedLinks = linkHelper.getLinkStreamFromProperty(allLinks)
                .map(Link::getHref)
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
        Optional<Link> linkOptional = linkHelper.getLinkStreamFromProperty(INVALID_INTERNAL).findFirst();

        assertFalse(linkOptional.isPresent());
    }

    @Test
    void shouldReturnEmptyForInvalidExternal() {
        Optional<Link> linkOptional = linkHelper.getLinkStreamFromProperty(INVALID_EXTERNAL).findFirst();

        assertFalse(linkOptional.isPresent());
    }

    @Test
    void testValidateInternalLink_valid() {
        context.create().resource(VALID_INTERNAL);

        LinkStatus linkStatus = linkHelper.validateInternalLink(VALID_INTERNAL, context.resourceResolver());

        testLinkStatus(HttpStatus.SC_OK, linkStatus);
    }

    @Test
    void testValidateInternalEncodedLink_valid() {
        context.create().resource(VALID_INTERNAL_DECODED);

        LinkStatus linkStatus = linkHelper.validateInternalLink(VALID_INTERNAL_ENCODED, context.resourceResolver());

        testLinkStatus(HttpStatus.SC_OK, linkStatus);
    }

    @Test
    void testValidateInternalLink_invalid() {
        LinkStatus linkStatus = linkHelper.validateInternalLink(VALID_INTERNAL, context.resourceResolver());

        testLinkStatus(HttpStatus.SC_NOT_FOUND, linkStatus);
    }

    @Test
    void testValidateInternalEncodedLink_decodeException() {
        try (MockedStatic<URLDecoder> urlDecoder = mockStatic(URLDecoder.class)) {
            urlDecoder.when(() ->
                    URLDecoder.decode(eq(VALID_INTERNAL_ENCODED), anyString())
            ).thenThrow(new UnsupportedEncodingException());

            LinkStatus linkStatus = linkHelper.validateInternalLink(VALID_INTERNAL_ENCODED, context.resourceResolver());

            testLinkStatus(HttpStatus.SC_NOT_FOUND, linkStatus);
        }
    }

    @Test
    void testValidateExternalLink_valid() throws IOException, URISyntaxException {
        when(externalLinkChecker.checkLink(VALID_EXTERNAL)).thenReturn(HttpStatus.SC_OK);

        LinkStatus linkStatus = linkHelper.validateExternalLink(VALID_EXTERNAL);

        testLinkStatus(HttpStatus.SC_OK, linkStatus);
    }

    @Test
    void testValidateExternalLink_notFound() throws IOException, URISyntaxException {
        when(externalLinkChecker.checkLink(INVALID_EXTERNAL)).thenReturn(HttpStatus.SC_NOT_FOUND);

        LinkStatus linkStatus = linkHelper.validateExternalLink(INVALID_EXTERNAL);

        testLinkStatus(HttpStatus.SC_NOT_FOUND, linkStatus);
    }

    @Test
    void testValidateExternalLink_socketTimeout() throws IOException, URISyntaxException {
        Exception e = new SocketTimeoutException();
        when(externalLinkChecker.checkLink(INVALID_EXTERNAL)).thenThrow(e);

        LinkStatus linkStatus = linkHelper.validateExternalLink(INVALID_EXTERNAL);

        testLinkStatus(HttpStatus.SC_REQUEST_TIMEOUT, e.toString(), linkStatus);
    }

    @Test
    void testValidateExternalLink_badRequest() throws IOException, URISyntaxException {
        Exception e = new IOException();
        when(externalLinkChecker.checkLink(INVALID_EXTERNAL)).thenThrow(e);

        LinkStatus linkStatus = linkHelper.validateExternalLink(INVALID_EXTERNAL);

        testLinkStatus(HttpStatus.SC_BAD_REQUEST, e.toString(), linkStatus);
    }

    @Test
    void testValidateLink_validInternal() {
        context.create().resource(VALID_INTERNAL);

        Link linkForValidation = new Link(VALID_INTERNAL, Link.Type.INTERNAL);
        LinkStatus linkStatus = linkHelper.validateLink(linkForValidation, context.resourceResolver());

        testLinkStatus(HttpStatus.SC_OK, linkStatus);
    }

    @Test
    void testValidateLink_invalidInternal() {
        Link linkForValidation = new Link(VALID_INTERNAL, Link.Type.INTERNAL);
        LinkStatus linkStatus = linkHelper.validateLink(linkForValidation, context.resourceResolver());

        testLinkStatus(HttpStatus.SC_NOT_FOUND, linkStatus);
    }

    @Test
    void testValidateLink_shouldSendRequestForInternal404LinksIfInternalLinksHostIsConfigured() throws NoSuchFieldException, URISyntaxException, IOException {
        PrivateAccessor.setField(linkHelper, INTERNAL_LINKS_HOST_FIELD, INTERNAL_LINKS_HOST_FIELD_VALUE);
        when(externalLinkChecker.checkLink(INTERNAL_LINKS_HOST_FIELD_VALUE + VALID_INTERNAL)).thenReturn(HttpStatus.SC_OK);
        Link linkForValidation = new Link(VALID_INTERNAL, Link.Type.INTERNAL);
        LinkStatus linkStatus = linkHelper.validateLink(linkForValidation, context.resourceResolver());

        testLinkStatus(HttpStatus.SC_OK, linkStatus);
    }

    @Test
    void testValidateLink_validExternal() throws IOException, URISyntaxException {
        when(externalLinkChecker.checkLink(VALID_EXTERNAL)).thenReturn(HttpStatus.SC_OK);

        Link linkForValidation = new Link(VALID_EXTERNAL, Link.Type.EXTERNAL);
        LinkStatus linkStatus = linkHelper.validateLink(linkForValidation, context.resourceResolver());

        testLinkStatus(HttpStatus.SC_OK, linkStatus);
    }

    @Test
    void testValidateLink_invalidExternal() throws IOException, URISyntaxException {
        when(externalLinkChecker.checkLink(INVALID_EXTERNAL)).thenReturn(HttpStatus.SC_NOT_FOUND);

        Link linkForValidation = new Link(INVALID_EXTERNAL, Link.Type.EXTERNAL);
        LinkStatus linkStatus = linkHelper.validateLink(linkForValidation, context.resourceResolver());

        testLinkStatus(HttpStatus.SC_NOT_FOUND, linkStatus);
    }

    @Test
    void testValidateLinkString_validInternal() {
        context.create().resource(VALID_INTERNAL);

        LinkStatus linkStatus = linkHelper.validateLink(VALID_INTERNAL, context.resourceResolver());

        testLinkStatus(HttpStatus.SC_OK, linkStatus);
    }

    @Test
    void testValidateLinkString_invalidInternal() {
        LinkStatus linkStatus = linkHelper.validateLink(VALID_INTERNAL, context.resourceResolver());

        testLinkStatus(HttpStatus.SC_NOT_FOUND, linkStatus);
    }

    @Test
    void testValidateLinkString_validExternal() throws IOException, URISyntaxException {
        when(externalLinkChecker.checkLink(VALID_EXTERNAL)).thenReturn(HttpStatus.SC_OK);

        LinkStatus linkStatus = linkHelper.validateLink(VALID_EXTERNAL, context.resourceResolver());

        testLinkStatus(HttpStatus.SC_OK, linkStatus);
    }

    @Test
    void testValidateLinkString_invalidExternal() throws IOException, URISyntaxException {
        when(externalLinkChecker.checkLink(VALID_EXTERNAL)).thenReturn(HttpStatus.SC_NOT_FOUND);

        LinkStatus linkStatus = linkHelper.validateLink(VALID_EXTERNAL, context.resourceResolver());

        testLinkStatus(HttpStatus.SC_NOT_FOUND, linkStatus);
    }

    @Test
    void testValidateLinkString_invalidSyntax() {
        String linkInsideText = String.format(SINGLE_LINK_HTML_TAG_PLACEHOLDER, VALID_INTERNAL);

        LinkStatus linkStatus = linkHelper.validateLink(linkInsideText, context.resourceResolver());

        testLinkStatus(HttpStatus.SC_BAD_REQUEST, INVALID_SYNTAX_MESSAGE, linkStatus);
    }

    @Test
    void testReplaceLink_singleValue() {
        Resource resource = context.create().resource(RESOURCE_PATH, PROPERTY_NAME, CURRENT_LINK);

        linkHelper.replaceLink(context.resourceResolver(), RESOURCE_PATH, PROPERTY_NAME, CURRENT_LINK, NEW_LINK);

        String updatedValue = resource.getValueMap().get(PROPERTY_NAME, String.class);
        assertEquals(NEW_LINK, updatedValue);
    }

    @Test
    void testReplaceLink_multiValue() {
        String[] multiValue = {CURRENT_LINK, CURRENT_LINK};
        Resource resource = context.create().resource(RESOURCE_PATH, PROPERTY_NAME, multiValue);

        linkHelper.replaceLink(context.resourceResolver(), RESOURCE_PATH, PROPERTY_NAME, CURRENT_LINK, NEW_LINK);

        String[] updatedValue = resource.getValueMap().get(PROPERTY_NAME, String[].class);
        assertTrue(Arrays.stream(updatedValue).allMatch(NEW_LINK::equals));
    }

    @Test
    void testReplaceLink_currentLinkNotFound() {
        Resource resource = context.create().resource(RESOURCE_PATH, PROPERTY_NAME, VALID_INTERNAL);

        linkHelper.replaceLink(context.resourceResolver(), RESOURCE_PATH, PROPERTY_NAME, CURRENT_LINK, NEW_LINK);

        String updatedValue = resource.getValueMap().get(PROPERTY_NAME, String.class);
        assertEquals(VALID_INTERNAL, updatedValue);
    }

    private void testLinkStatus(int expectedCode, LinkStatus linkStatus) {
        testLinkStatus(expectedCode, HttpStatus.getStatusText(expectedCode), linkStatus);
    }

    private void testLinkStatus(int expectedCode, String expectedMessage, LinkStatus linkStatus) {
        assertEquals(expectedCode, linkStatus.getStatusCode());
        assertEquals(expectedMessage, linkStatus.getStatusMessage());
    }

    private void testGetSingleLink(String link) {
        testGetSingleLinkFromText(link, link);
    }

    private void testGetSingleLinkFromText(String text, String expected) {
        Optional<Link> linkOptional = linkHelper.getLinkStreamFromProperty(text).findFirst();

        assertTrue(linkOptional.isPresent());
        assertEquals(expected, linkOptional.get().getHref());
    }
}