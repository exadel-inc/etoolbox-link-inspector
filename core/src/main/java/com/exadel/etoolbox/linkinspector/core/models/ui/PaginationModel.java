package com.exadel.etoolbox.linkinspector.core.models.ui;

import com.exadel.etoolbox.linkinspector.core.services.cache.GridResourcesCache;
import com.exadel.etoolbox.linkinspector.core.services.data.models.GridResource;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents the model with pagination logic.
 */
@Model(
        adaptables = SlingHttpServletRequest.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class PaginationModel {

    private static final String REQUEST_PARAMETER_PAGE = "page";
    private static final String REQUEST_PARAMETER_TYPE = "type";
    private static final String REQUEST_PARAMETER_SUBSTRING = "substring";
    private static final int DEFAULT_PAGE_NUMBER = 1;
    private static final int DEFAULT_PAGE_SIZE = 500;

    @OSGiService
    private GridResourcesCache cache;

    @SlingObject
    private ResourceResolver resourceResolver;

    @Self
    private SlingHttpServletRequest request;

    private int page;

    private int size;

    @PostConstruct
    private void init() {
        page = Optional
                .ofNullable(request.getRequestParameter(REQUEST_PARAMETER_PAGE))
                .map(RequestParameter::getString)
                .filter(NumberUtils::isNumber)
                .map(Integer::parseInt)
                .orElse(DEFAULT_PAGE_NUMBER);

        String type = requestParameterToString(request.getRequestParameter(REQUEST_PARAMETER_TYPE));
        String substring = requestParameterToString(request.getRequestParameter(REQUEST_PARAMETER_SUBSTRING));

        List<GridResource> resources = cache
                .getGridResourcesList()
                .stream()
                .filter(gridResource -> StringUtils.isBlank(type) || StringUtils.equals(gridResource.getLink().getType(), type))
                .filter(gridResource -> StringUtils.isBlank(substring) || gridResource.getLink().getHref().contains(substring))
                .collect(Collectors.toList());

        size = (int) Math.ceil((double) resources.size()/DEFAULT_PAGE_SIZE);
    }

    private String requestParameterToString(RequestParameter parameter) {
        return Optional
                .ofNullable(parameter)
                .map(RequestParameter::getString)
                .orElse(StringUtils.EMPTY);
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public int getPreviousPage() {
        return page - 1;
    }

    public int getPageBeforePrevious() {
        return page - 2;
    }

    public int getNextPage() {
        return page + 1;
    }

    public int getPageAfterPrevious() {
        return page + 2;
    }
}
