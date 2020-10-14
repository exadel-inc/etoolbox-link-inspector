package com.exadel.linkchecker.core.models;

import com.day.cq.commons.date.RelativeTimeFormat;
import com.day.cq.replication.ReplicationStatus;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.components.Component;
import com.day.cq.wcm.api.components.ComponentManager;
import com.exadel.linkchecker.core.services.util.GridResourceProperties;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import javax.annotation.PostConstruct;
import java.util.*;

@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class GridItem {

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
    private String componentName;
    private String componentPath;
    private String componentType;
    private Calendar lastReplicated;

    @PostConstruct
    private void init() {
        Resource resourceToShow = resourceResolver.getResource(path);

        ComponentManager componentManager = resourceResolver.adaptTo(ComponentManager.class);
        Component cpm = componentManager.getComponentOfResource(resourceToShow);
        componentName = cpm != null ? cpm.getTitle() : resourceToShow.getName();
        componentType = cpm != null ? cpm.getResourceType() : "";

        Page page = resourceResolver.adaptTo(PageManager.class).getContainingPage(resourceToShow);
        pagePath = page.getPath();

        lastReplicated = page.adaptTo(ReplicationStatus.class).getLastPublished();

        componentPath = path.replaceAll(":", "%3A");
    }

//    private void setDescription(List<String> languageCodes) {
//        if (languageCodes.size() < 10) {
//            description = String.join(", ", languageCodes);
//        } else {
//            description = languageCodes.size() + " languages";
//        }
//    }
//
//    private boolean isNewTranslationSinceLastReplication(Resource dictionaryKeyResource) {
//        Calendar created = dictionaryKeyResource.getValueMap().get("jcr:created", Calendar.class);
//        return lastReplicated == null || lastReplicated.before(created);
//    }

    public String getTitle() {
        return getPath();
    }

    public String getThumbnail() {
        return THUMBNAIL_PATH;
    }

    public String getPath() {
        return path;
    }

    public String getReplicationDate() {
        return formatDateRDF(lastReplicated, null);
    }

    private String formatDateRDF(Calendar cal, String defaultValue) {
        if (cal == null) {
            return defaultValue;
        }
        RelativeTimeFormat rtf = new RelativeTimeFormat("r");
        return rtf.format(cal.getTimeInMillis(), true);
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
}
