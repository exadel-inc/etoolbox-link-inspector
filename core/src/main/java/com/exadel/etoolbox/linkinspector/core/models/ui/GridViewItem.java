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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.HttpStatus;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Represents a row in the UI grid. The row consists of the information about links, such as type, href, status code,
 * status code, status message, along with the details about containing page and the component.
 */
@Model(
        adaptables = {SlingHttpServletRequest.class, Resource.class},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
@Getter
@Slf4j
public class GridViewItem {
    private static final Pattern EXCEPTION = Pattern.compile("(\\w+\\.)+\\w+");

    /**
     * The prefix for building a page link on Author instance
     */
    private static final String EDITOR_LINK = "/editor.html";

    /**
     * The CRX DE path prefix for building a component link
     */
    public static final String CRX_DE_PATH = "/crx/de/index.jsp#";

    private static final String DOT = ".";
    private static final String SLASH = "/";
    private static final String HTML_EXTENSION = ".html";
    private static final String COLON = ":";

    @SlingObject
    @Getter(value = AccessLevel.NONE)
    private ResourceResolver resourceResolver;

    @ValueMapValue
    private String value;

    @ValueMapValue
    private String match;

    @ValueMapValue
    private String type;

    @ValueMapValue
    private String statusCode;

    @ValueMapValue
    private String statusMessage;

    @ValueMapValue
    private String resourcePath;

    @ValueMapValue
    private String propertyName;

    private String pagePath;
    private String pageTitle;
    private String componentName;
    private String componentPath;
    private String componentType;
    private boolean isValidPage;

    @PostConstruct
    private void init() {
        Resource resourceToShow = StringUtils.isNotBlank(resourcePath)
                ? resourceResolver.getResource(resourcePath)
                : null;
        if (resourceToShow == null) {
            log.warn("Resource is null, path: {}", resourcePath);
            return;
        }

        Optional<Component> componentOptional = Optional.ofNullable(resourceResolver.adaptTo(ComponentManager.class))
                .map(componentManager -> componentManager.getComponentOfResource(resourceToShow));
        componentName = componentOptional.map(Component::getTitle).orElse(resourceToShow.getName());
        componentType = componentOptional.map(Component::getResourceType).orElse(resourceToShow.getResourceType());

        Optional<Page> pageOptional = Optional.ofNullable(resourceResolver.adaptTo(PageManager.class))
                .map(pageManager -> pageManager.getContainingPage(resourceToShow));
        pagePath = pageOptional.map(page -> EDITOR_LINK + page.getPath() + HTML_EXTENSION)
                .orElse(resourcePath);
        pageTitle = pageOptional.map(Page::getTitle).orElse(StringUtils.EMPTY);
        isValidPage = pageOptional.isPresent();

        componentPath = encodePath(resourcePath);
    }

    public String getStatusCode() {
        if (NumberUtils.isParsable(statusCode) && Integer.parseInt(statusCode) > 0) {
            return "HTTP " + statusCode;
        }
        return getStatusMessageExcerpt();
    }

    public String getStatusMessageExcerpt() {
        if (!isStatusClampable()) {
            return statusMessage;
        }
        if (StringUtils.contains(statusMessage, COLON)) {
            return StringUtils.substringAfterLast(statusMessage, COLON);
        }
        String result = StringUtils.substringAfterLast(statusMessage, DOT);
        return StringUtils.substringBefore(result, StringUtils.SPACE);
    }

    public String getStatusTag() {
        if (!NumberUtils.isParsable(statusCode) || "0".equals(statusCode)) {
            return "undefined";
        }
        int code = Integer.parseInt(statusCode);
        if (code >= HttpStatus.SC_OK && code < HttpStatus.SC_MULTIPLE_CHOICES) {
            return "ok";
        }
        return "error";
    }

    public String getTitle() {
        return getResourcePath();
    }

    public String getCrxDePath() {
        return CRX_DE_PATH;
    }

    public boolean isStatusClampable() {
        return StringUtils.contains(statusMessage, COLON)
                || (StringUtils.isNotEmpty(statusMessage) && EXCEPTION.matcher(statusMessage).find());
    }

    private static String encodePath(String path) {
        return Arrays.stream(path.split(SLASH))
                .map(JcrUtil::escapeIllegalJcrChars)
                .collect(Collectors.joining(SLASH));
    }
}
