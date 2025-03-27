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

import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.components.Component;
import com.day.cq.wcm.api.components.ComponentManager;
import com.exadel.etoolbox.linkinspector.core.services.data.models.GridResource;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Represents a row in the UI grid. The row consists of the information about links, such as type, href, status code,
 * status code, status message, along with the details about containing page and the component.
 */
@Model(
        adaptables = {SlingHttpServletRequest.class, Resource.class},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class GridViewItem {
    private static final Logger LOG = LoggerFactory.getLogger(GridViewItem.class);

    /**
     * The prefix for building a page link on Author instance
     */
    private static final String EDITOR_LINK = "/editor.html";

    /**
     * The CRX DE path prefix for building a component link
     */
    public static final String CRX_DE_PATH = "/crx/de/index.jsp#";

    public static final String SLASH_CHAR = "/";
    public static final String HTML_EXTENSION = ".html";

    @SlingObject
    private ResourceResolver resourceResolver;

    @ValueMapValue(name = GridResource.PN_LINK)
    private String result;

    @ValueMapValue(name = "match")
    private String matchedText;

    @ValueMapValue(name = GridResource.PN_LINK_TYPE)
    private String linkType;

    @ValueMapValue(name = GridResource.PN_LINK_STATUS_CODE)
    private String linkStatusCode;

    @ValueMapValue(name = GridResource.PN_LINK_STATUS_MESSAGE)
    private String linkStatusMessage;

    @ValueMapValue(name = GridResource.PN_RESOURCE_PATH)
    private String path;

    @ValueMapValue(name = GridResource.PN_PROPERTY_NAME)
    private String propertyName;

    private String pagePath;
    private String pageTitle;
    private String componentName;
    private String componentPath;
    private String componentType;
    private boolean isValidPage;

    @PostConstruct
    private void init() {
        Resource resourceToShow = resourceResolver.getResource(path);
        if (resourceToShow == null) {
            LOG.warn("Resource is null, path: {}", path);
            return;
        }

        Optional<Component> componentOptional = Optional.ofNullable(resourceResolver.adaptTo(ComponentManager.class))
                .map(componentManager -> componentManager.getComponentOfResource(resourceToShow));
        componentName = componentOptional.map(Component::getTitle).orElse(resourceToShow.getName());
        componentType = componentOptional.map(Component::getResourceType).orElse(resourceToShow.getResourceType());

        Optional<Page> pageOptional = Optional.ofNullable(resourceResolver.adaptTo(PageManager.class))
                .map(pageManager -> pageManager.getContainingPage(resourceToShow));
        pagePath = pageOptional.map(page -> EDITOR_LINK + page.getPath() + HTML_EXTENSION)
                .orElse(path);
        pageTitle = pageOptional.map(Page::getTitle).orElse(StringUtils.EMPTY);
        isValidPage = pageOptional.isPresent();

        componentPath = encodePath(path);
    }

    public String getTitle() {
        return getPath();
    }

    public String getPath() {
        return path;
    }

    public String getResult() {
        return result;
    }

    public String getMatchedText() {
        return matchedText;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getComponentName() {
        return componentName;
    }

    public String getPagePath() {
        return pagePath;
    }

    public String getComponentPath() {
        return componentPath;
    }

    public String getLinkType() {
        return linkType;
    }

    public String getLinkStatusCode() {
        return linkStatusCode;
    }

    public String getLinkStatusMessage() {
        return linkStatusMessage;
    }

    public String getComponentType() {
        return componentType;
    }

    public String getPageTitle() {
        return pageTitle;
    }

    public boolean isValidPage() {
        return isValidPage;
    }

    public String getCrxDePath() {
        return CRX_DE_PATH;
    }

    private static String encodePath(String path) {
        return Arrays.stream(path.split(SLASH_CHAR))
                .map(JcrUtil::escapeIllegalJcrChars)
                .collect(Collectors.joining(SLASH_CHAR));
    }
}
