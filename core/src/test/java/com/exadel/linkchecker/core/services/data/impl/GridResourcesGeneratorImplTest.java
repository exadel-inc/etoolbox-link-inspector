package com.exadel.linkchecker.core.services.data.impl;

import com.exadel.linkchecker.core.models.Link;
import com.exadel.linkchecker.core.services.ExternalLinkChecker;
import com.exadel.linkchecker.core.services.data.DataFeedService;
import com.exadel.linkchecker.core.services.data.models.GridResource;
import com.exadel.linkchecker.core.services.helpers.LinkHelper;
import com.exadel.linkchecker.core.services.helpers.RepositoryHelper;
import com.exadel.linkchecker.core.services.helpers.impl.LinkHelperImpl;
import com.exadel.linkchecker.core.services.helpers.impl.RepositoryHelperImpl;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import junitx.util.PrivateAccessor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(AemContextExtension.class)
class GridResourcesGeneratorImplTest {
    private static final String RESOURCE_RESOLVER_FACTORY_FIELD = "resourceResolverFactory";
    private static final String REPOSITORY_HELPER_FIELD = "repositoryHelper";
    private static final String LINK_HELPER_FIELD = "linkHelper";
    private static final String REAL_DATAFEED_PATH = "/apps/exadel-linkchecker/components/content/data/datafeed.json";
    private static final String EXECUTOR_SERVICE_FIELD = "executorService";
    private static final String EXTERNAL_LINK_CHECKER_FIELD = "externalLinkChecker";
    private static final String GRID_RESOURCE_TYPE = "exadel-linkchecker/components/gridConfig";

    private static final String TEST_DATAFEED_PATH = "/com/exadel/linkchecker/core/services/data/impl/expectedResources.json";
    private static final String TEST_RESOURCES_TREE_PATH = "/com/exadel/linkchecker/core/services/data/impl/resources.json";
    private static final String TEST_FOLDER_PATH = "/content/test-folder";
    private static final String TEST_EXCLUDED_PROPERTY = "excluded_prop";
    private static final String TEST_EXCLUDED_SITE = "linkedin.com";
    private static final String TEST_EXCLUDED_PATH = "/content/test-folder/excluded";
    private static final String TEST_EXCLUDED_LINK = "/content/test-link-excluded-1";
    private static final String TEST_EXCLUDED_CHILD_LINK = "/content/test-link-excluded-2";

    private final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    private final GridResourcesGeneratorImpl fixture = new GridResourcesGeneratorImpl();

    private ExternalLinkChecker externalLinkChecker;

    @BeforeEach
    void setup() throws NoSuchFieldException {
        LinkHelper linkHelper = new LinkHelperImpl();
        externalLinkChecker = mock(ExternalLinkChecker.class);
        PrivateAccessor.setField(linkHelper, EXTERNAL_LINK_CHECKER_FIELD, externalLinkChecker);

        PrivateAccessor.setField(fixture, LINK_HELPER_FIELD, linkHelper);
    }

    @Test
    void testGenerateGridResources() throws NoSuchFieldException, IOException, URISyntaxException {
        setUpConfig(fixture);
        context.load().json(TEST_RESOURCES_TREE_PATH, TEST_FOLDER_PATH);
        when(externalLinkChecker.checkLink(anyString())).thenReturn(HttpStatus.SC_NOT_FOUND);

        List<GridResource> gridResources = fixture.generateGridResources(GRID_RESOURCE_TYPE, context.resourceResolver());
        List<GridResource> expectedGridResources = buildExpectedGridResources();

        assertTrue(CollectionUtils.isEqualCollection(expectedGridResources, gridResources));
    }

    @Test
    void testAllowedStatusCodes() throws IOException, URISyntaxException {
        setUpConfig(fixture);
        context.load().json(TEST_RESOURCES_TREE_PATH, TEST_FOLDER_PATH);
        when(externalLinkChecker.checkLink(anyString())).thenReturn(HttpStatus.SC_BAD_REQUEST);

        List<GridResource> gridResources = fixture.generateGridResources(GRID_RESOURCE_TYPE, context.resourceResolver());
        boolean notContainsExternal = gridResources.stream()
                .map(GridResource::getLink)
                .map(Link::getType)
                .noneMatch(Link.Type.EXTERNAL::equals);

        assertTrue(notContainsExternal);
    }

    @Test
    void testAllowedStatusCodes_emptyConfig() throws IOException, URISyntaxException, NoSuchFieldException {
        setUpConfigNoStatusCodes(fixture);

        context.load().json(TEST_RESOURCES_TREE_PATH, TEST_FOLDER_PATH);
        when(externalLinkChecker.checkLink(anyString())).thenReturn(HttpStatus.SC_BAD_REQUEST);

        List<GridResource> gridResources = fixture.generateGridResources(GRID_RESOURCE_TYPE, context.resourceResolver());
        List<GridResource> expectedGridResources = buildExpectedGridResources();
        assertTrue(CollectionUtils.isEqualCollection(expectedGridResources, gridResources));
    }


    @Test
    void testExcludedPaths() throws IOException, URISyntaxException {
        setUpConfig(fixture);
        context.load().json(TEST_RESOURCES_TREE_PATH, TEST_FOLDER_PATH);
        when(externalLinkChecker.checkLink(anyString())).thenReturn(HttpStatus.SC_BAD_REQUEST);

        List<GridResource> gridResources = fixture.generateGridResources(GRID_RESOURCE_TYPE, context.resourceResolver());
        boolean notContainsExcluded = gridResources.stream()
                .map(GridResource::getHref)
                .noneMatch(href ->
                        href.equals(TEST_EXCLUDED_LINK) || href.equals(TEST_EXCLUDED_CHILD_LINK)
                );

        assertTrue(notContainsExcluded);
    }

    @Test
    void testExcludedPaths_emptyConfig() throws IOException, URISyntaxException, NoSuchFieldException {
        setUpConfigNoExcludedPaths(fixture);
        context.load().json(TEST_RESOURCES_TREE_PATH, TEST_FOLDER_PATH);
        when(externalLinkChecker.checkLink(anyString())).thenReturn(HttpStatus.SC_BAD_REQUEST);

        List<GridResource> gridResources = fixture.generateGridResources(GRID_RESOURCE_TYPE, context.resourceResolver());

        boolean containsExcluded = gridResources.stream()
                .map(GridResource::getHref)
                .anyMatch(TEST_EXCLUDED_LINK::equals);
        boolean containsExcludedChild = gridResources.stream()
                .map(GridResource::getHref)
                .anyMatch(TEST_EXCLUDED_CHILD_LINK::equals);

        assertTrue(containsExcluded && containsExcludedChild);
    }

    @Test
    void testGenerateGridResources_rootResourceNull() {
        setUpConfig(fixture);
        List<GridResource> gridResources = fixture.generateGridResources(GRID_RESOURCE_TYPE, context.resourceResolver());
        assertTrue(gridResources.isEmpty());
    }

    @Test
    void testGenerateGridResources_nothingFoundAfterTraversing() {
        setUpConfig(fixture);
        context.create().resource(TEST_FOLDER_PATH,
                JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, JcrResourceConstants.NT_SLING_FOLDER);

        List<GridResource> gridResources = fixture.generateGridResources(GRID_RESOURCE_TYPE, context.resourceResolver());
        assertTrue(gridResources.isEmpty());
    }

    @Test
    void testDeactivate() throws NoSuchFieldException {
        ExecutorService executorService = mock(ExecutorService.class);
        PrivateAccessor.setField(fixture, EXECUTOR_SERVICE_FIELD, executorService);

        fixture.deactivate();

        verify(executorService).shutdownNow();
    }

    @Test
    void testGenerateGridResources_interruptionException() throws InterruptedException {
        setUpConfig(fixture);
        context.load().json(TEST_RESOURCES_TREE_PATH, TEST_FOLDER_PATH);

        try (MockedStatic<Executors> executors = mockStatic(Executors.class)) {
            ExecutorService executorService = mock(ExecutorService.class);
            executors.when(() -> Executors.newFixedThreadPool(anyInt())).thenReturn(executorService);
            when(executorService.submit(any(Runnable.class))).thenReturn(null);
            when(executorService.awaitTermination(eq(Long.MAX_VALUE), eq(TimeUnit.NANOSECONDS))).thenThrow(new InterruptedException());

            fixture.generateGridResources(GRID_RESOURCE_TYPE, context.resourceResolver());

            verify(executorService).shutdownNow();
        }
    }

    static void setUpConfig(GridResourcesGeneratorImpl gridResourcesGenerator) {
        GridResourcesGeneratorImpl.Configuration config = mockConfig();

        int[] defaultStatusCodes = {HttpStatus.SC_NOT_FOUND};
        when(config.allowed_status_codes()).thenReturn(defaultStatusCodes);

        String[] excludedPaths = {TEST_EXCLUDED_PATH};
        when(config.excluded_paths()).thenReturn(excludedPaths);

        gridResourcesGenerator.activate(config);
    }

    private void setUpConfigNoExcludedPaths(GridResourcesGeneratorImpl gridResourcesGenerator) {
        GridResourcesGeneratorImpl.Configuration config = mockConfig();

        int[] defaultStatusCodes = {HttpStatus.SC_NOT_FOUND};
        when(config.allowed_status_codes()).thenReturn(defaultStatusCodes);

        gridResourcesGenerator.activate(config);
    }

    private void setUpConfigNoStatusCodes(GridResourcesGeneratorImpl gridResourcesGenerator) {
        GridResourcesGeneratorImpl.Configuration config = mockConfig();

        String[] excludedPaths = {TEST_EXCLUDED_PATH};
        when(config.excluded_paths()).thenReturn(excludedPaths);

        gridResourcesGenerator.activate(config);
    }

    private static GridResourcesGeneratorImpl.Configuration mockConfig() {
        GridResourcesGeneratorImpl.Configuration config = mock(GridResourcesGeneratorImpl.Configuration.class);
        when(config.search_path()).thenReturn(TEST_FOLDER_PATH);

        when(config.links_type()).thenReturn(StringUtils.EMPTY);
        when(config.threads_per_core()).thenReturn(60);

        String[] excludedProps = {TEST_EXCLUDED_PROPERTY};
        when(config.excluded_properties()).thenReturn(excludedProps);

        String[] excludedSites = {TEST_EXCLUDED_SITE};
        when(config.excluded_sites()).thenReturn(excludedSites);

        return config;
    }

    private List<GridResource> buildExpectedGridResources() throws NoSuchFieldException {
        context.load().binaryFile(TEST_DATAFEED_PATH, REAL_DATAFEED_PATH);
        DataFeedService dataFeedService = setUpDataFeedService(getRepositoryHelperFromContext());
        return dataFeedService.dataFeedToGridResources();
    }

    private DataFeedService setUpDataFeedService(RepositoryHelper repositoryHelper) throws NoSuchFieldException {
        DataFeedService dataFeedService = new DataFeedServiceImpl();
        PrivateAccessor.setField(dataFeedService, REPOSITORY_HELPER_FIELD, repositoryHelper);
        return dataFeedService;
    }

    private RepositoryHelper getRepositoryHelperFromContext() throws NoSuchFieldException {
        ResourceResolverFactory resourceResolverFactory = context.getService(ResourceResolverFactory.class);
        RepositoryHelper repositoryHelper = new RepositoryHelperImpl();
        PrivateAccessor.setField(repositoryHelper, RESOURCE_RESOLVER_FACTORY_FIELD, resourceResolverFactory);
        return repositoryHelper;
    }
}