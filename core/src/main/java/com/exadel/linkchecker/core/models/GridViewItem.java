package com.exadel.linkchecker.core.models;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.components.Component;
import com.day.cq.wcm.api.components.ComponentManager;
import com.exadel.linkchecker.core.services.util.GridResourceProperties;
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
import java.util.*;

@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class GridViewItem {
    private static final Logger LOG = LoggerFactory.getLogger(GridViewItem.class);

    public final static String EDITOR_LINK = "/editor.html";
    //todo - crxde is not available in AEMaaCS
    public final static String CRX_DE_LINK = "/crx/de/index.jsp#";

    public final static String THUMBNAIL_PATH = "/apps/linkchecker/components/thumb.png";

    @SlingObject
    private ResourceResolver resourceResolver;

    @ValueMapValue(name = GridResourceProperties.PN_LINK)
    private String link;

    @ValueMapValue(name = GridResourceProperties.PN_LINK_TYPE)
    private String linkType;

    @ValueMapValue(name = GridResourceProperties.PN_LINK_STATUS_CODE)
    private String linkStatusCode;

    @ValueMapValue(name = GridResourceProperties.PN_LINK_STATUS_MESSAGE)
    private String linkStatusMessage;

    @ValueMapValue(name = GridResourceProperties.PN_RESOURCE_PATH)
    private String path;

    @ValueMapValue(name = GridResourceProperties.PN_PROPERTY_NAME)
    private String propertyName;

    private String pagePath;
    private String pageTitle;
    private String componentName;
    private String componentPath;
    private String componentType;

    @PostConstruct
    private void init() {
        Resource resourceToShow = resourceResolver.getResource(path);
        if (resourceToShow == null) {
            LOG.warn("Resource for is null, path: {}", path);
            return;
        }

        Optional<Component> componentOptional = Optional.ofNullable(resourceResolver.adaptTo(ComponentManager.class))
                .map(componentManager -> componentManager.getComponentOfResource(resourceToShow));
        componentName = componentOptional.map(Component::getTitle).orElse(resourceToShow.getName());
        componentType = componentOptional.map(Component::getResourceType).orElse(StringUtils.EMPTY);

        Optional<Page> pageOptional = Optional.ofNullable(resourceResolver.adaptTo(PageManager.class))
                .map(pageManager -> pageManager.getContainingPage(resourceToShow));
        pagePath = pageOptional.map(page -> EDITOR_LINK + page.getPath()).orElse(path);
        pageTitle = pageOptional.map(Page::getTitle).orElse(path);

        //todo - use proper url encoding
        componentPath = path.replaceAll(":", "%3A");
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
}
