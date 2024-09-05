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

import com.exadel.etoolbox.linkinspector.core.services.data.DataFeedService;
import com.exadel.etoolbox.linkinspector.core.services.data.impl.DataFeedServiceImpl;
import com.exadel.etoolbox.linkinspector.core.services.helpers.LinkHelper;
import com.exadel.etoolbox.linkinspector.core.services.helpers.PackageHelper;
import com.exadel.etoolbox.linkinspector.core.services.helpers.RepositoryHelper;
import com.exadel.etoolbox.linkinspector.core.services.helpers.impl.LinkHelperImpl;
import com.exadel.etoolbox.linkinspector.core.services.helpers.impl.RepositoryHelperImpl;
import com.exadel.etoolbox.linkinspector.core.services.mocks.MockDataFeedService;
import com.exadel.etoolbox.linkinspector.core.services.mocks.MockHttpClientBuilderFactory;
import com.exadel.etoolbox.linkinspector.core.services.mocks.MockRepositoryHelper;
import com.exadel.etoolbox.linkinspector.core.services.resolvers.ExternalLinkResolverImpl;
import com.exadel.etoolbox.linkinspector.core.services.resolvers.InternalLinkResolverImpl;
import com.exadel.etoolbox.linkinspector.core.services.util.CsvUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import junitx.util.PrivateAccessor;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.http.HttpStatus;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.stubbing.Answer;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(AemContextExtension.class)
class ReplaceByPatternServletTest {
    private static final String DATAFEED_SERVICE_FIELD = "dataFeedService";
    private static final String LINK_HELPER_FIELD = "linkHelper";
    private static final String REPOSITORY_HELPER_FIELD = "repositoryHelper";
    private static final String RESOURCE_RESOLVER_FACTORY_FIELD = "resourceResolverFactory";

    private static final int DEFAULT_COMMIT_THRESHOLD = 1000;

    private static final String LINK_PATTERN_PARAM = "pattern";
    private static final String REPLACEMENT_PARAM = "replacement";
    private static final String DRY_RUN_PARAM = "isDryRun";
    private static final String BACKUP_PARAM = "isBackup";
    private static final String OUTPUT_AS_CSV_PARAM = "isOutputAsCsv";

    private static final String SELECTED_PARAM = "selected";

    private static final String TEST_LINK_PATTERN = "test-pattern";
    private static final String TEST_REPLACEMENT = "test-replacement";
    private static final String TEST_RESOURCE_PATH_1 = "/content/test-folder/test-resource1";
    private static final String TEST_PROPERTY_1 = "test1";
    private static final String TEST_RESOURCE_PATH_3 = "/content/test-folder/test-resource3";
    private static final String TEST_PROPERTY_3 = "test3";
    private static final String TEST_DATAFEED_PATH = "/com/exadel/etoolbox/linkinspector/core/servlets/datafeed.json";
    private static final String TEST_SELECTED_VALUES_PATH = "com/exadel/etoolbox/linkinspector/core/servlets/selected.json";
    private static final String REAL_DATAFEED_PATH = "/content/etoolbox-link-inspector/data/datafeed.json";
    private static final String TEST_RESOURCES_TREE_PATH = "/com/exadel/etoolbox/linkinspector/core/servlets/resources.json";
    private static final String TEST_FOLDER_PATH = "/content/test-folder";
    private static final String TEST_EXCEPTION_MSG = "Test exception message";
    private static final String ADVANCED_MODE = "advancedMode";

    private final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    private ReplaceByPatternServlet fixture;

    private LinkHelper linkHelper;
    private RepositoryHelper repositoryHelper;
    private PackageHelper packageHelper;

    private MockSlingHttpServletRequest request;
    private MockSlingHttpServletResponse response;

    @BeforeEach
    void setup() throws NoSuchFieldException {
        request = context.request();
        response = context.response();

        context.registerInjectActivateService(new MockDataFeedService());
        repositoryHelper = context.registerInjectActivateService(new MockRepositoryHelper(context.resourceResolver()));
        packageHelper = context.registerService(PackageHelper.class, mock(PackageHelper.class));

        context.registerInjectActivateService(
                new MockHttpClientBuilderFactory(),
                ImmutableMap.of(
                        "statusCode", org.apache.http.HttpStatus.SC_NOT_FOUND,
                        "statusMessage", "Not Found")
        );
        context.registerInjectActivateService(new ExternalLinkResolverImpl());
        context.registerInjectActivateService(new InternalLinkResolverImpl());
        linkHelper = context.registerInjectActivateService(new LinkHelperImpl());

        fixture = context.registerInjectActivateService(new ReplaceByPatternServlet());
    }

    @Test
    void testEmptyParams() {
        linkHelper = mock(LinkHelper.class);
        repositoryHelper = mock(RepositoryHelper.class);
        packageHelper = mock(PackageHelper.class);

        fixture.doPost(request, response);

        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        verifyNoInteractions(linkHelper);
        verifyNoInteractions(repositoryHelper);
        verifyNoInteractions(packageHelper);
    }

    @Test
    void testCurrentLinkEqualToReplacement() {
        request.addRequestParameter(LINK_PATTERN_PARAM, TEST_LINK_PATTERN);
        request.addRequestParameter(REPLACEMENT_PARAM, TEST_LINK_PATTERN);
        request.addRequestParameter(ADVANCED_MODE, Boolean.TRUE.toString());
        linkHelper = mock(LinkHelper.class);
        repositoryHelper = mock(RepositoryHelper.class);
        packageHelper = mock(PackageHelper.class);

        fixture.doPost(request, response);

        assertEquals(HttpStatus.SC_ACCEPTED, response.getStatus());
        verifyNoInteractions(linkHelper);
        verifyNoInteractions(repositoryHelper);
        verifyNoInteractions(packageHelper);
    }

    @Test
    void testNoUpdatedItems() throws IOException {
        linkHelper = mock(LinkHelper.class);
        repositoryHelper = mock(RepositoryHelper.class);
        packageHelper = mock(PackageHelper.class);
        setUpRequestParamsLinks();

        fixture.doPost(request, response);

        assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatus());
        verifyNoInteractions(linkHelper);
        verifyNoInteractions(repositoryHelper);
        verifyNoInteractions(packageHelper);
    }

    @Test
    void testReplacementSuccess() throws NoSuchFieldException, IOException {
        setUpHelpersResources();
        setUpRequestParamsLinks();

        fixture.doPost(request, response);

        assertEquals(1, ((MockRepositoryHelper) repositoryHelper).getCreationsCount());
        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertTrue(isReplacementDone(TEST_RESOURCE_PATH_1, TEST_PROPERTY_1, TEST_REPLACEMENT));
        assertTrue(isReplacementDone(TEST_RESOURCE_PATH_3, TEST_PROPERTY_3, TEST_REPLACEMENT));
    }

    @Test
    void testDeactivate() throws NoSuchFieldException, IOException {
        setUpHelpersResources();
        setUpRequestParamsLinks();
        packageHelper = mock(PackageHelper.class);
        linkHelper = mock(LinkHelper.class);

        fixture.deactivate();
        fixture.doPost(request, response);

        assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatus());
        verifyNoInteractions(packageHelper);
        verifyNoInteractions(linkHelper);
    }

    @Test
    void testDryRun() throws NoSuchFieldException, IOException {
        setUpDataFeedService(getRepositoryHelperFromContext());
        setUpCommitThreshold();
        setUpResources();

        SlingHttpServletRequest requestMock = mockSlingRequest();
        mockRequestParam(DRY_RUN_PARAM, Boolean.TRUE.toString(), requestMock);

        ResourceResolver resourceResolverMock = mockResourceResolver(requestMock);
        mockLinkHelper(resourceResolverMock);
        mockSession(resourceResolverMock);

        when(resourceResolverMock.hasChanges()).thenReturn(true);

        fixture.doPost(requestMock, response);

        assertEquals(0, ((MockRepositoryHelper) repositoryHelper).getCreationsCount());
        verify(resourceResolverMock, never()).commit();
    }

    @Test
    void testBackup() throws NoSuchFieldException, RepositoryException, PackageException, IOException {
        setUpHelpersResources();
        setUpRequestParamsWithBackup();

        fixture.doPost(request, response);

        verify(packageHelper).createPackageForPaths(
                anyCollection(),
                any(Session.class),
                anyString(),
                anyString(),
                anyString(),
                eq(true),
                eq(true));
        assertEquals(HttpStatus.SC_OK, response.getStatus());
    }

    @Test
    void testBackup_exception() throws NoSuchFieldException, RepositoryException, PackageException, IOException {
        setUpHelpersResources();
        setUpRequestParamsWithBackup();

        doThrow(new PackageException()).when(packageHelper)
                .createPackageForPaths(anyCollection(), any(Session.class), anyString(), anyString(), anyString(), eq(true), eq(true));

        fixture.doPost(request, response);

        assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatus());
    }

    @Test
    void testCsvOutput() throws NoSuchFieldException, IOException {
        setUpHelpersResources();
        setUpRequestParamsWithCsvOut();

        fixture.doPost(request, response);

        Assertions.assertEquals(CsvUtil.CSV_MIME_TYPE, response.getContentType());
        assertTrue(response.getContentLength() > 0);
        assertEquals(FileUploadBase.ATTACHMENT, response.getHeader(FileUploadBase.CONTENT_DISPOSITION));
        OutputStream outputStream = response.getOutputStream();
        assertNotNull(outputStream);
    }

    @Test
    void testCsvOutput_emptyByteArray() throws NoSuchFieldException, IOException {
        setUpHelpersResources();
        setUpRequestParamsWithCsvOut();

        try (MockedStatic<CsvUtil> csvUtil = mockStatic(CsvUtil.class)) {
            csvUtil.when(() -> CsvUtil.itemsToCsvByteArray(anyCollection(), any(BiConsumer.class), any(String[].class)))
                    .thenReturn(new byte[0]);
            fixture.doPost(request, response);
        }

        assertNotEquals(CsvUtil.CSV_MIME_TYPE, response.getContentType());
        assertNull(response.getHeader(FileUploadBase.CONTENT_DISPOSITION));
    }

    @Test
    void testCsvOutput_exception() throws NoSuchFieldException, IOException {
        setUpHelpersResources();
        setUpRequestParamsWithCsvOut();
        SlingHttpServletResponse responseMock = mock(SlingHttpServletResponse.class);
        ServletOutputStream outputStreamMock = mock(ServletOutputStream.class);

        when(responseMock.getOutputStream()).thenReturn(outputStreamMock);
        verifyNoMoreInteractions(responseMock);
        doThrow(new IOException()).when(outputStreamMock).flush();

        fixture.doPost(request, responseMock);
    }

    @Test
    void testCsvOutput_printItemException() throws NoSuchFieldException, IOException {
        setUpHelpersResources();
        setUpRequestParamsWithCsvOut();

        try (MockedStatic<CsvUtil> csvUtil = mockStatic(CsvUtil.class)) {
            Answer<Object> answer = invocationOnMock -> {
                throw new IOException();
            };
            csvUtil.when(() -> CsvUtil.itemsToCsvByteArray(anyCollection(), any(BiConsumer.class), any(String[].class)))
                    .thenCallRealMethod();
            csvUtil.when(() -> CsvUtil.wrapIfContainsSemicolon(anyString()))
                    .then(answer);
            fixture.doPost(request, response);
        }

        assertEquals(1, countLines(response.getOutputAsString()));
    }

    @Test
    void testEmptySession() throws IOException {
        SlingHttpServletRequest requestMock = mockSlingRequest();
        ResourceResolver resourceResolverMock = mockResourceResolver(requestMock);
        linkHelper = mock(LinkHelper.class);
        repositoryHelper = mock(RepositoryHelper.class);
        packageHelper = mock(PackageHelper.class);

        when(resourceResolverMock.adaptTo(Session.class)).thenReturn(null);

        fixture.doPost(requestMock, response);

        assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatus());
        verifyNoInteractions(linkHelper);
        verifyNoInteractions(repositoryHelper);
        verifyNoInteractions(packageHelper);
    }

    @Test
    void testReplacement_persistenceException() throws NoSuchFieldException, IOException {
        setUpDataFeedService(getRepositoryHelperFromContext());
        setUpCommitThreshold();
        setUpResources();

        SlingHttpServletRequest requestMock = mockSlingRequest();
        ResourceResolver resourceResolverMock = mockResourceResolver(requestMock);
        mockLinkHelper(resourceResolverMock);
        mockSession(resourceResolverMock);

        when(resourceResolverMock.hasChanges()).thenReturn(true);
        doThrow(new PersistenceException(TEST_EXCEPTION_MSG)).when(resourceResolverMock).commit();

        fixture.doPost(requestMock, response);

        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
    }

    @Test
    void testReplacement_commitThreshold() throws NoSuchFieldException, IOException {
        setUpDataFeedService(getRepositoryHelperFromContext());
        setUpCommitThreshold(1);
        setUpResources();

        SlingHttpServletRequest requestMock = mockSlingRequest();
        ResourceResolver resourceResolverMock = mockResourceResolver(requestMock);
        mockLinkHelper(resourceResolverMock);
        mockSession(resourceResolverMock);

        when(resourceResolverMock.hasChanges()).thenReturn(false);

        fixture.doPost(requestMock, response);

        verify(resourceResolverMock, atLeastOnce()).commit();
        assertEquals(HttpStatus.SC_OK, response.getStatus());
    }

    private void setUpHelpersResources() throws NoSuchFieldException {
        setUpDataFeedService(getRepositoryHelperFromContext());
        setUpCommitThreshold();
        setUpResources();
    }

    private RepositoryHelper getRepositoryHelperFromContext() throws NoSuchFieldException {
        ResourceResolverFactory resourceResolverFactory = context.getService(ResourceResolverFactory.class);
        RepositoryHelper repositoryHelper = new RepositoryHelperImpl();
        PrivateAccessor.setField(repositoryHelper, RESOURCE_RESOLVER_FACTORY_FIELD, resourceResolverFactory);
        return repositoryHelper;
    }

    private void setUpDataFeedService(RepositoryHelper repositoryHelper) throws NoSuchFieldException {
        DataFeedService dataFeedService = new DataFeedServiceImpl();
        PrivateAccessor.setField(dataFeedService, REPOSITORY_HELPER_FIELD, repositoryHelper);
        PrivateAccessor.setField(fixture, DATAFEED_SERVICE_FIELD, dataFeedService);
    }

    private void setUpCommitThreshold() {
        setUpCommitThreshold(DEFAULT_COMMIT_THRESHOLD);
    }

    private void setUpCommitThreshold(int commitThreshold) {
        ReplaceByPatternServlet.Configuration config = mock(ReplaceByPatternServlet.Configuration.class);
        when(config.commitThreshold()).thenReturn(commitThreshold);

        fixture.activate(config);
    }

    private void setUpResources() {
        context.load().binaryFile(TEST_DATAFEED_PATH, REAL_DATAFEED_PATH);
        context.load().json(TEST_RESOURCES_TREE_PATH, TEST_FOLDER_PATH);
    }

    private void setUpRequestParamsWithCsvOut() throws IOException {
        setUpRequestParamsLinks();
        request.addRequestParameter(OUTPUT_AS_CSV_PARAM, Boolean.TRUE.toString());
    }

    private void setUpRequestParamsWithBackup() throws IOException {
        setUpRequestParamsLinks();
        request.addRequestParameter(BACKUP_PARAM, Boolean.TRUE.toString());
    }

    private void setUpRequestParamsLinks() throws IOException {
        request.addRequestParameter(LINK_PATTERN_PARAM, TEST_LINK_PATTERN);
        request.addRequestParameter(REPLACEMENT_PARAM, TEST_REPLACEMENT);
        request.addRequestParameter(ADVANCED_MODE, Boolean.TRUE.toString());
        Arrays.stream(loadSelectedValues()).forEach(value ->
                request.addRequestParameter(SELECTED_PARAM, value)
        );
    }

    private SlingHttpServletRequest mockSlingRequest() throws IOException {
        SlingHttpServletRequest requestMock = mock(SlingHttpServletRequest.class);
        mockRequestParam(LINK_PATTERN_PARAM, TEST_LINK_PATTERN, requestMock);
        mockRequestParam(REPLACEMENT_PARAM, TEST_REPLACEMENT, requestMock);
        mockRequestParams(SELECTED_PARAM, loadSelectedValues(), requestMock);
        return requestMock;
    }

    private void mockSession(ResourceResolver resourceResolverMock) {
        Session sessionMock = mock(Session.class);
        when(resourceResolverMock.adaptTo(Session.class)).thenReturn(sessionMock);
    }

    private ResourceResolver mockResourceResolver(SlingHttpServletRequest requestMock) {
        ResourceResolver resourceResolverMock = mock(ResourceResolver.class);
        when(requestMock.getResourceResolver()).thenReturn(resourceResolverMock);
        return resourceResolverMock;
    }

    private void mockLinkHelper(ResourceResolver resourceResolverMock) throws NoSuchFieldException {
        LinkHelper linkHelper = mock(LinkHelper.class);
        PrivateAccessor.setField(fixture, LINK_HELPER_FIELD, linkHelper);
        when(linkHelper.replaceLink(eq(resourceResolverMock), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(true);
    }

    private void mockRequestParam(String paramName, String value, SlingHttpServletRequest requestMock) {
        RequestParameter requestParameter = mock(RequestParameter.class);
        when(requestMock.getRequestParameter(paramName)).thenReturn(requestParameter);
        when(requestParameter.getString()).thenReturn(value);
    }

    private boolean isReplacementDone(String resourcePath, String property, String replacement) {
        return Optional.ofNullable(context.resourceResolver().getResource(resourcePath))
                .map(Resource::getValueMap)
                .map(valueMap -> valueMap.get(property, String.class))
                .filter(href -> href.contains(replacement))
                .isPresent();
    }

    private int countLines(String str) {
        String[] lines = str.split("\r\n|\r|\n");
        return lines.length;
    }

    private String[] loadSelectedValues() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(TEST_SELECTED_VALUES_PATH).getFile());
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(file, String[].class);
    }

    private void mockRequestParams(String paramName, String[] values, SlingHttpServletRequest requestMock) {
        List<RequestParameter> requestParameters = new ArrayList<>();
        Arrays.stream(values).forEach(value -> {
                    RequestParameter requestParameter = mock(RequestParameter.class);
                    when(requestParameter.getString()).thenReturn(value);
                    requestParameters.add(requestParameter);
                }
        );
        when(requestMock.getRequestParameters(paramName)).thenReturn(requestParameters.toArray(new RequestParameter[0]));
    }
}