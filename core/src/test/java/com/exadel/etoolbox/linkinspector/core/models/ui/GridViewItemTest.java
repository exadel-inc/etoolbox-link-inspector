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

package com.exadel.etoolbox.linkinspector.core.models.ui;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.Page;
import com.exadel.etoolbox.linkinspector.core.services.data.models.GridResource;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import junitx.util.PrivateAccessor;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(AemContextExtension.class)
public class GridViewItemTest {
    private static final String MODELS_PACKAGE = "com.exadel.etoolbox.linkinspector.core.models";
    private static final String COMPONENT_TYPE_FIELD = "componentType";
    private static final String HTML_EXTENSION = ".html";
    private static final String EDITOR_LINK = "/editor.html";

    private static final String TEST_COMPONENT_TYPE = "/apps/etoolbox-link-inspector/components/test-component";
    private static final String TEST_COMPONENT_NAME = "Test Component";

    private static final String TEST_PAGE_NAME = "test-page";
    private static final String TEST_PAGE_PATH = String.join("/", "/content", TEST_PAGE_NAME);

    private static final String TEST_RESOURCE_NAME = "test-res-with-broken-link";
    private static final String TEST_RESOURCE_PATH =
            String.join("/", TEST_PAGE_PATH, JcrConstants.JCR_CONTENT, TEST_RESOURCE_NAME);
    private static final String TEST_RESOURCE_PATH_ENCODED =
            String.join("/", TEST_PAGE_PATH, JcrConstants.JCR_CONTENT.replace(":", "%3A"), TEST_RESOURCE_NAME);

    private static final String TEST_NON_EXISTING_RESOURCE_PATH = "/content/non-existing-resource-path";
    private static final String TEST_GRID_RESOURCE_PATH = "/content/grid-resource";

    private static final String TEST_PROPERTY = "test-prop-with-broken-link";

    private static final String TEST_BROKEN_LINK_HREF = "/content/internal-test-link";
    private static final String TEST_BROKEN_LINK_TYPE = "internal";
    private static final String TEST_BROKEN_LINK_SC = "HTTP " + HttpStatus.SC_NOT_FOUND;
    private static final String TEST_BROKEN_LINK_STATUS_MESSAGE = HttpStatus.getStatusText(HttpStatus.SC_NOT_FOUND);

    private final AemContext context = new AemContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    private GridViewItem viewItem;
    private Resource resourceWithBrokenLink;

    @BeforeEach
    public void setup() {
        context.addModelsForPackage(MODELS_PACKAGE);
    }

    @Test
    public void testPath() {
        setupFullParamSet();
        assertEquals(TEST_RESOURCE_PATH, viewItem.getResourcePath());
    }

    @Test
    void testTitle() {
        setupFullParamSet();
        assertEquals(TEST_RESOURCE_PATH, viewItem.getTitle());
    }

    @Test
    void testLink() {
        setupFullParamSet();
        assertEquals(TEST_BROKEN_LINK_HREF, viewItem.getValue());
    }

    @Test
    void testPropertyName() {
        setupFullParamSet();
        assertEquals(TEST_PROPERTY, viewItem.getPropertyName());
    }

    @Test
    void testComponentName() {
        setupFullParamSet();
        assertEquals(TEST_COMPONENT_NAME, viewItem.getComponentName());
    }

    @Test
    void testComponentNameIfComponentNotFound() {
        setupPageAndResource();
        setupGridResourceAndViewItem(resourceWithBrokenLink.getPath());
        assertEquals(resourceWithBrokenLink.getName(), viewItem.getComponentName());
    }

    @Test
    void testComponentTypeIfComponentNotFound() {
        setupPageAndResource();
        setupGridResourceAndViewItem(resourceWithBrokenLink.getPath());
        assertEquals(resourceWithBrokenLink.getResourceType(), viewItem.getComponentType());
    }

    @Test
    void testPagePathIfPageNotFound() {
        setupResource();
        setupGridResourceAndViewItem(resourceWithBrokenLink.getPath());
        assertEquals(resourceWithBrokenLink.getPath(), viewItem.getPagePath());
    }

    @Test
    void testPageTitleIfPageNotFound() {
        setupResource();
        setupGridResourceAndViewItem(resourceWithBrokenLink.getPath());
        assertEquals(StringUtils.EMPTY, viewItem.getPageTitle());
    }

    @Test
    void testPagePath() {
        setupFullParamSet();
        String expectedPath = EDITOR_LINK + TEST_PAGE_PATH + HTML_EXTENSION;
        assertEquals(expectedPath, viewItem.getPagePath());
    }

    @Test
    void testComponentPath() {
        setupFullParamSet();
        assertEquals(TEST_RESOURCE_PATH_ENCODED, viewItem.getComponentPath());
    }

    @Test
    void testLinkType() {
        setupFullParamSet();
        assertEquals(TEST_BROKEN_LINK_TYPE, viewItem.getType());
    }

    @Test
    void testLinkStatusCode() {
        setupFullParamSet();
        assertEquals(TEST_BROKEN_LINK_SC, viewItem.getStatusCode());
    }

    @Test
    void testLinkStatusMessage() {
        setupFullParamSet();
        assertEquals(TEST_BROKEN_LINK_STATUS_MESSAGE, viewItem.getStatusMessage());
    }

    @Test
    void testComponentType() throws NoSuchFieldException {
        setupFullParamSet();
        PrivateAccessor.setField(viewItem, COMPONENT_TYPE_FIELD, resourceWithBrokenLink.getResourceType());
        assertEquals(TEST_COMPONENT_TYPE, viewItem.getComponentType());
    }

    @Test
    void testPageTitle() {
        setupFullParamSet();
        assertEquals(TEST_PAGE_NAME, viewItem.getPageTitle());
    }

    @Test
    void isValidPage() {
        setupFullParamSet();
        assertTrue(viewItem.isValidPage());
    }

    @Test
    void shouldResourceExist() {
        setupFullParamSet();
        assertNotNull(viewItem.getResourcePath());
        assertNotNull(context.resourceResolver().getResource(viewItem.getResourcePath()));
    }

    @Test
    void shouldResourceNotExist() {
        setupGridResourceAndViewItem(TEST_NON_EXISTING_RESOURCE_PATH);
        assertNotNull(viewItem.getResourcePath());
        assertNull(context.resourceResolver().getResource(viewItem.getResourcePath()));
    }

    private void setupFullParamSet() {
        createComponentResource();
        setupPageAndResource();
        setupGridResourceAndViewItem(resourceWithBrokenLink.getPath());
    }

    private void createComponentResource() {
        context.create().resource(TEST_COMPONENT_TYPE,
                JcrConstants.JCR_PRIMARYTYPE, NameConstants.NT_COMPONENT,
                JcrConstants.JCR_TITLE, TEST_COMPONENT_NAME
        );
    }

    private void setupPageAndResource() {
        Page page = context.create().page(TEST_PAGE_PATH);
        resourceWithBrokenLink = context.create().resource(page, TEST_RESOURCE_NAME,
                JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, TEST_COMPONENT_TYPE,
                TEST_PROPERTY, TEST_BROKEN_LINK_HREF);
    }

    private void setupResource() {
        resourceWithBrokenLink = context.create().resource(TEST_RESOURCE_PATH,
                JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, TEST_COMPONENT_TYPE,
                TEST_PROPERTY, TEST_BROKEN_LINK_HREF);
    }

    private void setupGridResourceAndViewItem(String resourceWithBrokenLinkPath) {
        GridResource gridResourceModel = GridResource
                .builder()
                .resourcePath(resourceWithBrokenLinkPath)
                .propertyName(TEST_PROPERTY)
                .value(TEST_BROKEN_LINK_HREF)
                .type(TEST_BROKEN_LINK_TYPE)
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .statusMessage(TEST_BROKEN_LINK_STATUS_MESSAGE)
                .build();
        Resource gridResource = context
                .create()
                .resource(TEST_GRID_RESOURCE_PATH, gridResourceModel.toMap());
        viewItem = gridResource.adaptTo(GridViewItem.class);
    }
}