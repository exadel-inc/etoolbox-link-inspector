package com.exadel.etoolbox.linkinspector.core.services.helpers.impl;

import com.exadel.etoolbox.linkinspector.core.models.ui.GridViewItem;
import com.exadel.etoolbox.linkinspector.core.services.data.models.GridResource;
import com.exadel.etoolbox.linkinspector.core.services.helpers.CsvHelper;
import com.exadel.etoolbox.linkinspector.core.services.util.CsvUtil;
import com.exadel.etoolbox.linkinspector.core.services.util.LinkInspectorResourceUtil;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(AemContextExtension.class)
public class CsvHelperImplTest {

    private static final String VIEW_ITEMS_RESOURCES_PATH =
            "/com/exadel/etoolbox/linkinspector/core/services/data/impl/viewItems.json";
    private static final String TEST_RESOURCES_TREE_PATH = "/com/exadel/etoolbox/linkinspector/core/services/data/impl/resources.json";
    private static final String TEST_FOLDER_PATH = "/content/test-folder";
    private static final String TEST_VIEW_ITEMS_PATH = "/content/view-items";
    private static final String TEST_CSV_REPORT_PATH_1 = "/com/exadel/etoolbox/linkinspector/core/services/data/impl/expectedResources/1.csv";
    private static final String REAL_CSV_REPORT_PATH = "/content/etoolbox-link-inspector/data/content/1.csv";

    private final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    private final CsvHelper csvHelper = new CsvHelperImpl();

    @Test
    void shouldReadCsvReport() {
        context.load().binaryFile(TEST_CSV_REPORT_PATH_1, REAL_CSV_REPORT_PATH);
        List<GridResource> gridResourcesPage = csvHelper.readCsvReport(context.resourceResolver(), 1);
        assertFalse(gridResourcesPage.isEmpty());
    }

    @Test
    void shouldGenerateCsvReport() {

        context.load().json(TEST_RESOURCES_TREE_PATH, TEST_FOLDER_PATH);
        context.load().json(VIEW_ITEMS_RESOURCES_PATH, TEST_VIEW_ITEMS_PATH);

        Resource rootResource = context.resourceResolver().getResource(TEST_VIEW_ITEMS_PATH);

        List<GridViewItem> gridViewItems = loadGridViewItems(rootResource).stream()
                .map(resource -> resource.adaptTo(GridViewItem.class))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        try (MockedStatic<LinkInspectorResourceUtil> resourceUtil = mockStatic(LinkInspectorResourceUtil.class)) {
            csvHelper.generateCsvReport(context.resourceResolver(), gridViewItems);
            resourceUtil.verify(Mockito.times(1), () ->
                    LinkInspectorResourceUtil.saveFileToJCR(anyString(), any(), eq(CsvUtil.CSV_MIME_TYPE), any(ResourceResolver.class)));
        }
    }

    @Test
    void shouldSaveCsvReport() {

        context.load().json(TEST_RESOURCES_TREE_PATH, TEST_FOLDER_PATH);
        context.load().json(VIEW_ITEMS_RESOURCES_PATH, TEST_VIEW_ITEMS_PATH);

        Resource rootResource = context.resourceResolver().getResource(TEST_VIEW_ITEMS_PATH);

        List<GridViewItem> gridViewItems = loadGridViewItems(rootResource).stream()
                .map(resource -> resource.adaptTo(GridViewItem.class))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        try (MockedStatic<LinkInspectorResourceUtil> resourceUtil = mockStatic(LinkInspectorResourceUtil.class)) {
            csvHelper.saveCsvReport(context.resourceResolver(), gridViewItems, 1);
            resourceUtil.verify(Mockito.times(1), () ->
                    LinkInspectorResourceUtil.saveFileToJCR(anyString(), any(), eq(CsvUtil.CSV_MIME_TYPE), any(ResourceResolver.class)));
        }
    }

    private List<Resource> loadGridViewItems(Resource rootResource) {
        List<Resource> resources = new ArrayList<>();
        for (Resource resource : rootResource.getChildren()) {
            resources.add(resource);
        }
        return resources;
    }
}
