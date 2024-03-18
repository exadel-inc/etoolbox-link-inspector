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

import com.day.cq.replication.ReplicationStatus;
import com.exadel.etoolbox.linkinspector.core.models.Link;
import com.exadel.etoolbox.linkinspector.core.services.ExternalLinkChecker;
import com.exadel.etoolbox.linkinspector.core.services.data.GenerationStatsProps;
import com.exadel.etoolbox.linkinspector.core.services.data.UiConfigService;
import com.exadel.etoolbox.linkinspector.core.services.helpers.LinkHelper;
import com.exadel.etoolbox.linkinspector.core.services.data.DataFeedService;
import com.exadel.etoolbox.linkinspector.core.services.data.models.GridResource;
import com.exadel.etoolbox.linkinspector.core.services.helpers.RepositoryHelper;
import com.exadel.etoolbox.linkinspector.core.services.helpers.impl.LinkHelperImpl;
import com.exadel.etoolbox.linkinspector.core.services.helpers.impl.RepositoryHelperImpl;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import junitx.util.PrivateAccessor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(AemContextExtension.class)
class GridResourcesGeneratorImplTest {
    private static final String RESOURCE_RESOLVER_FACTORY_FIELD = "resourceResolverFactory";
    private static final String REPOSITORY_HELPER_FIELD = "repositoryHelper";
    private static final String LINK_HELPER_FIELD = "linkHelper";
    private static final String UI_CONFIG_FIELD = "uiConfigService";
    private static final String REAL_DATAFEED_PATH = "/content/etoolbox-link-inspector/data/datafeed.json";
    private static final String EXECUTOR_SERVICE_FIELD = "executorService";
    private static final String EXTERNAL_LINK_CHECKER_FIELD = "externalLinkChecker";
    private static final String GRID_RESOURCE_TYPE = "etoolbox-link-inspector/components/gridConfig";

    private static final String TEST_DATAFEED_PATH = "/com/exadel/etoolbox/linkinspector/core/services/data/impl/expectedResources.json";
    private static final String TEST_RESOURCES_TREE_PATH = "/com/exadel/etoolbox/linkinspector/core/services/data/impl/resources.json";
    private static final String TEST_FOLDER_PATH = "/content/test-folder";
    private static final String TEST_EXCLUDED_PROPERTY = "excluded_prop";
    private static final ZonedDateTime TEST_LAST_MODIFIED_BOUNDARY = ZonedDateTime.parse("2021-04-05T05:00:00Z", DateTimeFormatter.ISO_DATE_TIME);
    private static final String TEST_EXCLUDED_PATTERN = "/content(.*)/test-exclude-pattern(.*)";
    private static final String TEST_UI_EXCLUDED_PATTERN = "/content(.*)/test-link-internal-1$";
    private static final String TEST_EXCLUDED_PATH = "/content/test-folder/excluded_by_path";
    private static final String TEST_EXCLUDED_LINK = "/content/test-link-excluded-1";
    private static final String TEST_EXCLUDED_CHILD_LINK = "/content/test-link-excluded-2";
    private static final String TEST_EXCLUDED_BY_LAST_MODIFIED = "/content/test-link-excluded-last-modified";

    /**
     * Constants related to replication status check
     */
    private static final String TEST_REPLICATED_RESOURCES_TREE_PATH =
            "/com/exadel/etoolbox/linkinspector/core/services/data/impl/replicatedResources.json";
    private static final String LINK_PART_INACTIVE = "-inactive";
    private static final String TEST_PAGE_ACTIVE_RES = "test-page-active";
    private static final String TEST_PAGE_INACTIVE_RES_1 = "test-page-inactive-1";
    private static final String TEST_PAGE_INACTIVE_RES_2 = "test-page-inactive-2";
    private static final String TEST_COMPONENT_RES = "test-component";
    private static final String TEST_COMPONENT_INACTIVE_RES = "test-component-inactive";
    private static final String TEST_ASSET_RES = "test-asset-active";
    private static final String TEST_ASSET_INACTIVE_RES = "test-asset-inactive";
    private static final String TEST_PAGE_ACTIVE_AFTER_MODIFIED = "test-page-active-after-modified";
    private static final String TEST_PAGE_ACTIVE_BEFORE_MODIFIED_1 = "test-page-active-before-modified-1";
    private static final String TEST_PAGE_ACTIVE_BEFORE_MODIFIED_2 = "test-page-active-before-modified-2";
    private static final String TEST_ACTIVE_RES_1 = "test-resource-active-1";
    private static final String TEST_ACTIVE_RES_2 = "test-resource-active-2";
    private static final String TEST_ACTIVE_RES_3 = "test-resource-active-3";
    private static final String JCR_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final String TEST_LAST_REPLICATED = "2021-06-29T21:23:32.056Z";
    private static final List<String> EXPECTED_LINKS_ACTIVE = Arrays.asList(
            "/content/test-link-active-0",
            "/content/test-link-active-1",
            "/content/test-link-active-2",
            "/content/test-link-active-3",
            "/content/test-link-active-4",
            "/content/test-link-active-5",
            "/content/test-link-active-6",
            "/content/test-link-active-7",
            "/content/test-link-active-8",
            "/content/test-link-active-9",
            "/content/test-link-active-10"
    );

    private final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    private final GridResourcesGeneratorImpl fixture = new GridResourcesGeneratorImpl();

    private ExternalLinkChecker externalLinkChecker;
    private UiConfigService uiConfigService;

    @BeforeEach
    void setup() throws NoSuchFieldException {
        LinkHelper linkHelper = new LinkHelperImpl();
        externalLinkChecker = mock(ExternalLinkChecker.class);
        PrivateAccessor.setField(linkHelper, EXTERNAL_LINK_CHECKER_FIELD, externalLinkChecker);

        PrivateAccessor.setField(fixture, LINK_HELPER_FIELD, linkHelper);

        uiConfigService = mock(UiConfigServiceImpl.class);
        when(uiConfigService.getExcludedLinksPatterns()).thenReturn(new String[]{TEST_EXCLUDED_PATTERN});
        when(uiConfigService.getSearchPath()).thenReturn(TEST_FOLDER_PATH);
        when(uiConfigService.getExcludedPaths()).thenReturn(new String[]{TEST_EXCLUDED_PATH});
        when(uiConfigService.getLastModified()).thenReturn(TEST_LAST_MODIFIED_BOUNDARY);
        when(uiConfigService.getExcludedProperties()).thenReturn(new String[]{TEST_EXCLUDED_PROPERTY});
        when(uiConfigService.getLinksType()).thenReturn(GenerationStatsProps.REPORT_LINKS_TYPE_ALL);
        PrivateAccessor.setField(fixture, UI_CONFIG_FIELD, uiConfigService);
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
    void testGenerateFilteredGridResources() throws NoSuchFieldException, IOException, URISyntaxException {
        setUpConfig(fixture);
        context.load().json(TEST_RESOURCES_TREE_PATH, TEST_FOLDER_PATH);
        when(externalLinkChecker.checkLink(anyString())).thenReturn(HttpStatus.SC_NOT_FOUND);
        when(uiConfigService.getExcludedLinksPatterns()).thenReturn(new String[]{TEST_UI_EXCLUDED_PATTERN, TEST_EXCLUDED_PATTERN});

        List<GridResource> gridResources = fixture.generateGridResources(GRID_RESOURCE_TYPE, context.resourceResolver());
        Pattern pattern = Pattern.compile(TEST_UI_EXCLUDED_PATTERN);

        List<GridResource> expectedGridResources = buildExpectedGridResources().stream()
            .filter(gr -> {
                Matcher matcher = pattern.matcher(gr.getLink().getHref());
                return !matcher.matches();
            }).collect(Collectors.toList());

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
    void testLastModified() {
        setUpConfig(fixture);
        context.load().json(TEST_RESOURCES_TREE_PATH, TEST_FOLDER_PATH);

        List<GridResource> gridResources = fixture.generateGridResources(GRID_RESOURCE_TYPE, context.resourceResolver());
        boolean notContainsExcluded = gridResources.stream()
                .map(GridResource::getHref)
                .noneMatch(TEST_EXCLUDED_BY_LAST_MODIFIED::equals);

        assertTrue(notContainsExcluded);
    }

    @Test
    void testExcludedPaths_emptyConfig() throws IOException, URISyntaxException {
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
    void testActivationCheck() throws ParseException {
        setUpConfigCheckActivation(fixture);
        context.load().json(TEST_REPLICATED_RESOURCES_TREE_PATH, TEST_FOLDER_PATH);

        Resource rootResource = context.resourceResolver().getResource(TEST_FOLDER_PATH);
        assertNotNull(rootResource);
        Resource spyRootResource = initSpyResources(rootResource);

        ResourceResolver spyResourceResolver = spy(context.resourceResolver());
        doReturn(spyRootResource).when(spyResourceResolver).getResource(TEST_FOLDER_PATH);

        List<GridResource> gridResources = fixture.generateGridResources(GRID_RESOURCE_TYPE, spyResourceResolver);

        boolean notContainsInactive = gridResources.stream()
                .map(GridResource::getHref)
                .noneMatch(href -> href.contains(LINK_PART_INACTIVE));

        List<String> resultLinks = gridResources.stream()
                .map(GridResource::getHref)
                .collect(Collectors.toList());

        assertTrue(notContainsInactive);
        assertTrue(CollectionUtils.isEqualCollection(EXPECTED_LINKS_ACTIVE, resultLinks));
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
        when(config.allowedStatusCodes()).thenReturn(defaultStatusCodes);
        gridResourcesGenerator.activate(config);
    }

    private void setUpConfigNoExcludedPaths(GridResourcesGeneratorImpl gridResourcesGenerator) {
        GridResourcesGeneratorImpl.Configuration config = mockConfig();

        when(uiConfigService.getExcludedPaths()).thenReturn(ArrayUtils.EMPTY_STRING_ARRAY);

        int[] defaultStatusCodes = {HttpStatus.SC_NOT_FOUND};
        when(config.allowedStatusCodes()).thenReturn(defaultStatusCodes);

        gridResourcesGenerator.activate(config);
    }

    private void setUpConfigCheckActivation(GridResourcesGeneratorImpl gridResourcesGenerator) {
        GridResourcesGeneratorImpl.Configuration config = mockConfig();

        int[] defaultStatusCodes = {HttpStatus.SC_NOT_FOUND};
        when(config.allowedStatusCodes()).thenReturn(defaultStatusCodes);

        when(uiConfigService.getExcludedPaths()).thenReturn(new String[]{TEST_EXCLUDED_PATH});
        when(uiConfigService.isActivatedContent()).thenReturn(true);
        when(uiConfigService.isSkipContentModifiedAfterActivation()).thenReturn(true);

        gridResourcesGenerator.activate(config);
    }

    private void setUpConfigNoStatusCodes(GridResourcesGeneratorImpl gridResourcesGenerator) {
        GridResourcesGeneratorImpl.Configuration config = mockConfig();

        when(config.allowedStatusCodes()).thenReturn(ArrayUtils.EMPTY_INT_ARRAY);

        gridResourcesGenerator.activate(config);
    }

    private static GridResourcesGeneratorImpl.Configuration mockConfig() {
        GridResourcesGeneratorImpl.Configuration config = mock(GridResourcesGeneratorImpl.Configuration.class);

        when(config.threadsPerCore()).thenReturn(60);
        when(config.excludeTags()).thenReturn(true);

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

    private Resource initSpyResources(Resource rootResource) throws ParseException {
        Resource spyRootResource = spy(rootResource);

        Resource activePageRes = getChildSpyResource(rootResource, TEST_PAGE_ACTIVE_RES);
        Resource inactivePageRes1 = getChildSpyResource(rootResource, TEST_PAGE_INACTIVE_RES_1);
        Resource inactivePageRes2 = getChildSpyResource(activePageRes, TEST_PAGE_INACTIVE_RES_2);

        Resource componentRes1 = getChildSpyResource(rootResource, TEST_COMPONENT_RES);
        Resource componentRes2 = getChildSpyResource(rootResource, TEST_COMPONENT_INACTIVE_RES);
        Resource activeAssetRes = getChildSpyResource(rootResource, TEST_ASSET_RES);
        Resource inactiveAssetRes = getChildSpyResource(rootResource, TEST_ASSET_INACTIVE_RES);

        Resource pageActivateAfterModified = getChildSpyResource(rootResource, TEST_PAGE_ACTIVE_AFTER_MODIFIED);
        Resource pageActivateBeforeModified1 = getChildSpyResource(rootResource, TEST_PAGE_ACTIVE_BEFORE_MODIFIED_1);
        Resource pageActivateBeforeModified2 = getChildSpyResource(rootResource, TEST_PAGE_ACTIVE_BEFORE_MODIFIED_2);
        Resource activeRes1 = getChildSpyResource(rootResource, TEST_ACTIVE_RES_1);
        Resource activeRes2 = getChildSpyResource(rootResource, TEST_ACTIVE_RES_2);
        Resource activeRes3 = getChildSpyResource(rootResource, TEST_ACTIVE_RES_3);

        Iterator<Resource> spyChildrenResources = Arrays.asList(inactivePageRes1, activePageRes, componentRes1,
                componentRes2, activeAssetRes, inactiveAssetRes, pageActivateAfterModified, pageActivateBeforeModified1,
                pageActivateBeforeModified2, activeRes1, activeRes2, activeRes3).iterator();
        doReturn(spyChildrenResources).when(spyRootResource).listChildren();

        ReplicationStatus replicationStatusActivate = mock(ReplicationStatus.class);
        when(replicationStatusActivate.isActivated()).thenReturn(true);

        Calendar lastReplicated = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(JCR_DATE_FORMAT);
        lastReplicated.setTime(sdf.parse(TEST_LAST_REPLICATED));
        when(replicationStatusActivate.getLastPublished()).thenReturn(lastReplicated);

        ReplicationStatus replicationStatusDeactivate = mock(ReplicationStatus.class);
        when(replicationStatusDeactivate.isActivated()).thenReturn(false);

        doReturn(replicationStatusDeactivate).when(inactivePageRes1).adaptTo(ReplicationStatus.class);
        doReturn(replicationStatusActivate).when(activePageRes).adaptTo(ReplicationStatus.class);
        doReturn(replicationStatusDeactivate).when(inactivePageRes2).adaptTo(ReplicationStatus.class);

        doReturn(replicationStatusActivate).when(activeAssetRes).adaptTo(ReplicationStatus.class);
        doReturn(replicationStatusDeactivate).when(inactiveAssetRes).adaptTo(ReplicationStatus.class);

        doReturn(replicationStatusActivate).when(pageActivateAfterModified).adaptTo(ReplicationStatus.class);
        doReturn(replicationStatusActivate).when(pageActivateBeforeModified1).adaptTo(ReplicationStatus.class);
        doReturn(replicationStatusActivate).when(pageActivateBeforeModified2).adaptTo(ReplicationStatus.class);

        return spyRootResource;
    }

    private Resource getChildSpyResource(Resource resource, String childName) {
        Resource child = resource.getChild(childName);
        assertNotNull(child);
        return spy(child);
    }
}