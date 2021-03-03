package com.exadel.aembox.linkchecker.core.models.ui;

import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.components.Component;
import com.day.cq.wcm.api.components.ComponentManager;
import com.exadel.aembox.linkchecker.core.services.data.models.GridResource;
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
 * The model represents a separate row in the grid.
 */
@Model(
        adaptables = {SlingHttpServletRequest.class, Resource.class},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class GridViewItem {
    private static final Logger LOG = LoggerFactory.getLogger(GridViewItem.class);

    public static final String EDITOR_LINK = "/editor.html";
    //Note: CRX DE is not available in AEMaaCS
    public static final String CRX_DE_LINK = "/crx/de/index.jsp#";

    public static final String THUMBNAIL_PATH =
            "/etc.clientlibs/aembox-linkchecker/clientlibs/link-checker-ui/resources/thumbnail.png";
    public static final String SLASH_CHAR = "/";
    public static final String HTML_EXTENSION = ".html";

    @SlingObject
    private ResourceResolver resourceResolver;

    @ValueMapValue(name = GridResource.PN_LINK)
    private String link;

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

    public String getThumbnail() {
        return THUMBNAIL_PATH;
    }

    public String getPath() {
        return path;
    }

    public String getLink() {
        return link;
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

    private static String encodePath(String path) {
        return Arrays.stream(path.split(SLASH_CHAR))
                .map(JcrUtil::escapeIllegalJcrChars)
                .collect(Collectors.joining(SLASH_CHAR));
    }
}
