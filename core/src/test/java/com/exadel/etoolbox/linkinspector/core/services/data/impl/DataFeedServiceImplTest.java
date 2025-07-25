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

package com.exadel.etoolbox.linkinspector.core.services.data.impl;

import com.exadel.etoolbox.linkinspector.api.Resolver;
import com.exadel.etoolbox.linkinspector.core.services.cache.GridResourcesCache;
import com.exadel.etoolbox.linkinspector.core.services.cache.impl.GridResourcesCacheImpl;
import com.exadel.etoolbox.linkinspector.core.services.data.ConfigService;
import com.exadel.etoolbox.linkinspector.core.services.data.GridResourcesGenerator;
import com.exadel.etoolbox.linkinspector.core.services.data.models.DataFilter;
import com.exadel.etoolbox.linkinspector.core.services.data.models.GridResource;
import com.exadel.etoolbox.linkinspector.core.services.helpers.LinkHelper;
import com.exadel.etoolbox.linkinspector.core.services.helpers.RepositoryHelper;
import com.exadel.etoolbox.linkinspector.core.services.helpers.impl.LinkHelperImpl;
import com.exadel.etoolbox.linkinspector.core.services.helpers.impl.RepositoryHelperImpl;
import com.exadel.etoolbox.linkinspector.core.services.resolvers.ExternalLinkResolverImpl;
import com.exadel.etoolbox.linkinspector.core.services.resolvers.InternalLinkResolverImpl;
import com.exadel.etoolbox.linkinspector.core.services.util.CsvUtil;
import com.exadel.etoolbox.linkinspector.core.services.util.LinkInspectorResourceUtil;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import junitx.util.PrivateAccessor;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(AemContextExtension.class)
class DataFeedServiceImplTest {
    private static final String GRID_RESOURCES_GENERATOR_FIELD = "gridResourcesGenerator";
    private static final String RESOURCE_RESOLVER_FACTORY_FIELD = "resourceResolverFactory";
    private static final String GRID_RESOURCES_CACHE_FIELD = "gridResourcesCache";
    private static final String REPOSITORY_HELPER_FIELD = "repositoryHelper";
    private static final String LINK_HELPER_FIELD = "linkHelper";
    private static final String CONFIG_FIELD = "configService";

    private static final String DATAFEED_PATH = "/var/etoolbox/link-inspector/data/datafeed.json";
    private static final String CSV_REPORT_PATH = "/var/etoolbox/link-inspector/download/report.csv";

    private static final String TEST_RESOURCES_TREE_PATH = "/com/exadel/etoolbox/linkinspector/core/services/data/impl/resources.json";
    private static final String TEST_FOLDER_PATH = "/content/test-folder";

    private final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    private final DataFeedServiceImpl fixture = new DataFeedServiceImpl();

    @BeforeEach
    void setup() throws NoSuchFieldException, IOException, URISyntaxException {
        PrivateAccessor.setField(fixture, REPOSITORY_HELPER_FIELD, getRepositoryHelperFromContext());
        PrivateAccessor.setField(fixture, GRID_RESOURCES_CACHE_FIELD, getGridResourcesCacheFromContext());
        GridResourcesGeneratorImpl gridResourcesGenerator = getGridResourcesGenerator();
        PrivateAccessor.setField(fixture, GRID_RESOURCES_GENERATOR_FIELD, gridResourcesGenerator);
    }

    @Test
    void testGenerateDataFeed() {
        context.load().json(TEST_RESOURCES_TREE_PATH, TEST_FOLDER_PATH);

        fixture.generateDataFeed();

        Resource resource = context.resourceResolver().getResource(DATAFEED_PATH);
        assertNotNull(resource);
    }

    @Test
    void testGenerateDataFeed_nullResourceResolver() throws NoSuchFieldException {
        RepositoryHelper repositoryHelperMock = mock(RepositoryHelper.class);
        PrivateAccessor.setField(fixture, REPOSITORY_HELPER_FIELD, repositoryHelperMock);

        GridResourcesGenerator gridResourcesGenerator = mock(GridResourcesGenerator.class);
        PrivateAccessor.setField(fixture, GRID_RESOURCES_GENERATOR_FIELD, gridResourcesGenerator);

        when(repositoryHelperMock.getServiceResourceResolver()).thenReturn(null);

        fixture.generateDataFeed();
        verifyNoInteractions(gridResourcesGenerator);
    }

    @Test
    void testDataFeedToResources_nullResourceResolver() throws NoSuchFieldException {
        RepositoryHelper repositoryHelperMock = mock(RepositoryHelper.class);
        PrivateAccessor.setField(fixture, REPOSITORY_HELPER_FIELD, repositoryHelperMock);

        GridResourcesGenerator gridResourcesGenerator = mock(GridResourcesGenerator.class);
        PrivateAccessor.setField(fixture, GRID_RESOURCES_GENERATOR_FIELD, gridResourcesGenerator);

        when(repositoryHelperMock.getServiceResourceResolver()).thenReturn(null);

        List<Resource> resources = fixture.dataFeedToResources(new DataFilter());
        assertTrue(resources.isEmpty());
    }

    @Test
    void testDataFeedToGridResources_nullResourceResolver() throws NoSuchFieldException {
        RepositoryHelper repositoryHelperMock = mock(RepositoryHelper.class);
        PrivateAccessor.setField(fixture, REPOSITORY_HELPER_FIELD, repositoryHelperMock);

        GridResourcesGenerator gridResourcesGenerator = mock(GridResourcesGenerator.class);
        PrivateAccessor.setField(fixture, GRID_RESOURCES_GENERATOR_FIELD, gridResourcesGenerator);

        when(repositoryHelperMock.getServiceResourceResolver()).thenReturn(null);

        List<GridResource> gridResources = fixture.dataFeedToGridResources();
        assertTrue(gridResources.isEmpty());
    }

    @Test
    void testDataFeedToGridResources() {
        context.load().json(TEST_RESOURCES_TREE_PATH, TEST_FOLDER_PATH);
        fixture.generateDataFeed();

        List<GridResource> gridResources = fixture.dataFeedToGridResources();
        assertNotNull(gridResources);
    }

    @Test
    void testDataFeedToResources() {
        context.load().json(TEST_RESOURCES_TREE_PATH, TEST_FOLDER_PATH);
        fixture.generateDataFeed();

        assertNotNull(fixture.dataFeedToResources(new DataFilter()));
    }

    @Test
    void testGenerateCsv_printItemException() {
        context.load().json(TEST_RESOURCES_TREE_PATH, TEST_FOLDER_PATH);

        try (MockedStatic<CsvUtil> csvUtil = mockStatic(CsvUtil.class);
             MockedStatic<LinkInspectorResourceUtil> resourceUtil = mockStatic(LinkInspectorResourceUtil.class)) {
            Answer<Object> answer = invocationOnMock -> {
                throw new IOException();
            };
            csvUtil.when(() -> CsvUtil.itemsToCsvByteArray(anyCollection(), any(BiConsumer.class), any(String[].class)))
                    .thenCallRealMethod();
            csvUtil.when(() -> CsvUtil.wrapIfContainsSemicolon(anyString()))
                    .then(answer);

            fixture.generateDataFeed();

            resourceUtil.verify(() ->
                    LinkInspectorResourceUtil.removeResource(eq(CSV_REPORT_PATH), any(ResourceResolver.class)));
            resourceUtil.verify(() ->
                    LinkInspectorResourceUtil.saveFileToJCR(eq(CSV_REPORT_PATH), any(byte[].class), anyString(), any(ResourceResolver.class))
            );
        }
    }

    private GridResourcesGeneratorImpl getGridResourcesGenerator() throws NoSuchFieldException {
        GridResourcesGeneratorImpl gridResourcesGenerator = new GridResourcesGeneratorImpl();

        List<Resolver> linkResolvers = Arrays.asList(
                new ExternalLinkResolverImpl(),
                new InternalLinkResolverImpl()
        );
        LinkHelper linkHelper = new LinkHelperImpl();
        PrivateAccessor.setField(linkHelper, "linkResolvers", linkResolvers);
        PrivateAccessor.setField(gridResourcesGenerator, LINK_HELPER_FIELD, linkHelper);

        ConfigService configService = mock(ConfigServiceImpl.class);
        when(configService.getExcludedLinksPatterns()).thenReturn(new String[0]);
        when(configService.getSearchPath()).thenReturn(TEST_FOLDER_PATH);
        when(configService.getExcludedPaths()).thenReturn(new String[0]);
        when(configService.getExcludedProperties()).thenReturn(new String[0]);
        when(configService.getStatusCodes()).thenReturn(new int[]{HttpStatus.SC_NOT_FOUND});
        when(configService.getThreadsPerCore()).thenReturn(60);
        PrivateAccessor.setField(gridResourcesGenerator, CONFIG_FIELD, configService);

        return gridResourcesGenerator;
    }

    private RepositoryHelper getRepositoryHelperFromContext() throws NoSuchFieldException {
        ResourceResolverFactory resourceResolverFactory = context.getService(ResourceResolverFactory.class);
        RepositoryHelper repositoryHelper = new RepositoryHelperImpl();
        PrivateAccessor.setField(repositoryHelper, RESOURCE_RESOLVER_FACTORY_FIELD, resourceResolverFactory);
        return repositoryHelper;
    }

    private GridResourcesCache getGridResourcesCacheFromContext() throws NoSuchFieldException {
        GridResourcesCache gridResourcesCache = new GridResourcesCacheImpl();
        ConcurrentHashMap<String, CopyOnWriteArrayList<GridResource>> cache = new ConcurrentHashMap<>();
        PrivateAccessor.setField(gridResourcesCache, GRID_RESOURCES_CACHE_FIELD, cache);
        return gridResourcesCache;
    }
}