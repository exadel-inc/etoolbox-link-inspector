package com.exadel.linkchecker.core.services.data.impl;

import com.exadel.linkchecker.core.services.ExternalLinkChecker;
import com.exadel.linkchecker.core.services.data.GridResourcesGenerator;
import com.exadel.linkchecker.core.services.data.models.GridResource;
import com.exadel.linkchecker.core.services.helpers.LinkHelper;
import com.exadel.linkchecker.core.services.helpers.RepositoryHelper;
import com.exadel.linkchecker.core.services.helpers.impl.LinkHelperImpl;
import com.exadel.linkchecker.core.services.helpers.impl.RepositoryHelperImpl;
import com.exadel.linkchecker.core.services.util.CsvUtil;
import com.exadel.linkchecker.core.services.util.LinkCheckerResourceUtil;
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
import java.util.List;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(AemContextExtension.class)
class DataFeedServiceImplTest {
    private static final String GRID_RESOURCES_GENERATOR_FIELD = "gridResourcesGenerator";
    private static final String RESOURCE_RESOLVER_FACTORY_FIELD = "resourceResolverFactory";
    private static final String REPOSITORY_HELPER_FIELD = "repositoryHelper";
    private static final String LINK_HELPER_FIELD = "linkHelper";
    private static final String EXTERNAL_LINK_CHECKER_FIELD = "externalLinkChecker";

    private static final String DATAFEED_PATH = "/apps/exadel-linkchecker/components/content/data/datafeed.json";
    private static final String CSV_REPORT_PATH = "/content/exadel-linkchecker/download/report.csv";

    private static final String TEST_RESOURCES_TREE_PATH = "/com/exadel/linkchecker/core/services/data/impl/resources.json";
    private static final String TEST_FOLDER_PATH = "/content/test-folder";

    private final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    private final DataFeedServiceImpl fixture = new DataFeedServiceImpl();


    @BeforeEach
    void setup() throws NoSuchFieldException, IOException, URISyntaxException {
        PrivateAccessor.setField(fixture, REPOSITORY_HELPER_FIELD, getRepositoryHelperFromContext());

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

        List<Resource> resources = fixture.dataFeedToResources();
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

        assertNotNull(fixture.dataFeedToResources());
    }

    @Test
    void testGenerateCsv_printItemException() {
        context.load().json(TEST_RESOURCES_TREE_PATH, TEST_FOLDER_PATH);

        try (MockedStatic<CsvUtil> csvUtil = mockStatic(CsvUtil.class);
             MockedStatic<LinkCheckerResourceUtil> resourceUtil = mockStatic(LinkCheckerResourceUtil.class)) {
            Answer<Object> answer = invocationOnMock -> {
                throw new IOException();
            };
            csvUtil.when(() -> CsvUtil.itemsToCsvByteArray(anyCollection(), any(BiConsumer.class), any(String[].class)))
                    .thenCallRealMethod();
            csvUtil.when(() -> CsvUtil.wrapIfContainsSemicolon(anyString()))
                    .then(answer);

            fixture.generateDataFeed();

            resourceUtil.verify(() ->
                    LinkCheckerResourceUtil.removeResource(eq(CSV_REPORT_PATH), any(ResourceResolver.class)));
            resourceUtil.verify(() ->
                    LinkCheckerResourceUtil.saveFileToJCR(eq(CSV_REPORT_PATH), any(byte[].class), anyString(), any(ResourceResolver.class))
            );
        }
    }

    private GridResourcesGeneratorImpl getGridResourcesGenerator() throws NoSuchFieldException, IOException, URISyntaxException {
        GridResourcesGeneratorImpl gridResourcesGenerator = new GridResourcesGeneratorImpl();
        LinkHelper linkHelper = new LinkHelperImpl();
        ExternalLinkChecker externalLinkChecker = mock(ExternalLinkChecker.class);
        PrivateAccessor.setField(linkHelper, EXTERNAL_LINK_CHECKER_FIELD, externalLinkChecker);
        PrivateAccessor.setField(gridResourcesGenerator, LINK_HELPER_FIELD, linkHelper);
        GridResourcesGeneratorImplTest.setUpConfig(gridResourcesGenerator);

        when(externalLinkChecker.checkLink(anyString())).thenReturn(HttpStatus.SC_NOT_FOUND);

        return gridResourcesGenerator;
    }

    private RepositoryHelper getRepositoryHelperFromContext() throws NoSuchFieldException {
        ResourceResolverFactory resourceResolverFactory = context.getService(ResourceResolverFactory.class);
        RepositoryHelper repositoryHelper = new RepositoryHelperImpl();
        PrivateAccessor.setField(repositoryHelper, RESOURCE_RESOLVER_FACTORY_FIELD, resourceResolverFactory);
        return repositoryHelper;
    }
}