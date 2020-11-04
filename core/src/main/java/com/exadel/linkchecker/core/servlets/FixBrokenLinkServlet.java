package com.exadel.linkchecker.core.servlets;

import com.day.crx.JcrConstants;
import com.exadel.linkchecker.core.models.Link;
import com.exadel.linkchecker.core.services.LinkHelper;
import com.exadel.linkchecker.core.services.util.constants.CommonConstants;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.util.Arrays;
import java.util.Optional;

@Component(service = {Servlet.class})
@SlingServletResourceTypes(
        resourceTypes = "/bin/exadel/fix-broken-link",
        methods = HttpConstants.METHOD_POST
)
public class FixBrokenLinkServlet extends SlingAllMethodsServlet {
    private static final Logger LOG = LoggerFactory.getLogger(FixBrokenLinkServlet.class);

    private static final String PATH_PARAM = "path";
    private static final String PROPERTY_NAME_PARAM = "propertyName";
    private static final String CURRENT_LINK_PARAM = "currentLink";
    private static final String NEW_LINK_PARAM = "newLink";

    @Reference
    private LinkHelper linkHelper;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        try {
            String path = getRequestParamString(request, PATH_PARAM);
            String propertyName = getRequestParamString(request, PROPERTY_NAME_PARAM);
            String currentLink = getRequestParamString(request, CURRENT_LINK_PARAM);
            String newLink = getRequestParamString(request, NEW_LINK_PARAM);

            if (StringUtils.isAnyBlank(path, propertyName, currentLink, newLink)) {
                response.setStatus(HttpStatus.SC_BAD_REQUEST);
                LOG.warn("Any (or all) request params are empty: path - {}, propertyName - {}, currentLink - {}, newLink - {}",
                        path, propertyName, currentLink, newLink);
                return;
            }
            if (currentLink.equals(newLink)) {
                response.setStatus(HttpStatus.SC_ACCEPTED);
                LOG.debug("currentLink and newLink values are equal, no processing is required");
                return;
            }
            ResourceResolver resourceResolver = request.getResourceResolver();
            if (replaceLink(resourceResolver, path, propertyName, currentLink, newLink)) {
                ResourceUtil.getOrCreateResource(
                        resourceResolver,
                        CommonConstants.PENDING_GENERATION_NODE,
                        JcrConstants.NT_UNSTRUCTURED,
                        JcrResourceConstants.NT_SLING_FOLDER,
                        false
                );
                resourceResolver.commit();
                LOG.debug("The link was updated: path - {}, propertyName - {}, currentLink - {}, newLink - {}",
                        path, propertyName, currentLink, newLink);
            } else {
                LOG.debug("The current link {} was not updated", currentLink);
                response.setStatus(HttpStatus.SC_NO_CONTENT);
            }
        } catch (PersistenceException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private boolean replaceLink(ResourceResolver resourceResolver,
                                String path, String propertyName, String currentLink, String newLink) {
        return Optional.of(path)
                .map(resourceResolver::getResource)
                .map(resource -> resource.adaptTo(ModifiableValueMap.class))
                .map(modifiableValueMap ->
                        updateValueMapWithNewLink(modifiableValueMap, propertyName, currentLink, newLink)
                )
                .orElse(false);
    }

    private boolean updateValueMapWithNewLink(ModifiableValueMap modifiableValueMap,
                                              String propertyName, String currentLink, String newLink) {
        boolean updated = false;
        Optional<Object> updatedValue = Optional.ofNullable(modifiableValueMap.get(propertyName))
                .map(value -> getUpdatedValue(value, currentLink, newLink));
        if (updatedValue.isPresent()) {
            modifiableValueMap.put(propertyName, updatedValue.get());
            updated = true;
        }
        return updated;
    }

    private Object getUpdatedValue(Object value, String currentLink, String newLink) {
        Optional<String> currentLinkToReplace = linkHelper.getLinkStream(value)
                .map(Link::getHref)
                .filter(currentLink::equals)
                .findFirst();
        if (currentLinkToReplace.isPresent()) {
            if (value instanceof String) {
                String currentValue = (String) value;
                String newValue = currentValue.replaceAll(currentLinkToReplace.get(), newLink);
                if (!currentValue.equals(newValue)) {
                    return newValue;
                }
            } else if (value instanceof String[]) {
                String[] currentValues = (String[]) value;
                String[] newValues = Arrays.stream(currentValues)
                        .map(currentValue -> currentValue.replaceAll(currentLinkToReplace.get(), newLink))
                        .toArray(String[]::new);
                if (!Arrays.equals(currentValues, newValues)) {
                    return newValues;
                }
            }
        }
        return null;
    }

    private String getRequestParamString(SlingHttpServletRequest request, String param) {
        return Optional.ofNullable(request.getRequestParameter(param))
                .map(RequestParameter::getString)
                .orElse(StringUtils.EMPTY);
    }
}
