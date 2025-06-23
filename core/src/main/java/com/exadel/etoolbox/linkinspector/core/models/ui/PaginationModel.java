package com.exadel.etoolbox.linkinspector.core.models.ui;

import com.exadel.etoolbox.linkinspector.core.services.cache.GridResourcesCache;
import com.exadel.etoolbox.linkinspector.core.services.data.models.GridResource;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p><u>Note</u>: This class is not a part of the public API and is subject to change. Do not use it in your own code</p>
 * Calculates the pagination logic for the Link Inspector grid view.
 * This model handles the pagination implementation for the grid display, including
 * page number calculation, filtering by link type or substring, and determining
 * the range of pages to show in the pagination UI.
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
    public static final int DEFAULT_PAGE_SIZE = 50;

    @OSGiService
    private GridResourcesCache cache;

    @Self
    private SlingHttpServletRequest request;

    @Getter
    private int page;

    @Getter
    private int size;

    @PostConstruct
    private void init() {
        assert request != null;
        page = Optional
                .ofNullable(request.getRequestParameter(REQUEST_PARAMETER_PAGE))
                .map(RequestParameter::getString)
                .filter(NumberUtils::isNumber)
                .map(Integer::parseInt)
                .orElse(DEFAULT_PAGE_NUMBER);

        String type = requestParameterToString(request.getRequestParameter(REQUEST_PARAMETER_TYPE));
        String substring = requestParameterToString(request.getRequestParameter(REQUEST_PARAMETER_SUBSTRING));

        List<GridResource> resources = cache == null
                ? Collections.emptyList()
                : cache
                .getGridResourcesList()
                .stream()
                .filter(gridResource -> StringUtils.isBlank(type) || StringUtils.equals(gridResource.getType(), type))
                .filter(gridResource -> StringUtils.isBlank(substring) || StringUtils.contains(gridResource.getValue(), substring))
                .collect(Collectors.toList());

        size = resources.size() / DEFAULT_PAGE_SIZE + (resources.size() % DEFAULT_PAGE_SIZE == 0 ? 0 : 1);
    }

    private String requestParameterToString(RequestParameter parameter) {
        return Optional
                .ofNullable(parameter)
                .map(RequestParameter::getString)
                .orElse(StringUtils.EMPTY);
    }

    /**
     * Returns the previous page number.
     *
     * @return a non-negative integer
     */
    public int getPreviousPage() {
        return page - 1;
    }

    /**
     * Returns the page number before the previous page.
     *
     * @return a non-negative integer
     */
    public int getPageBeforePrevious() {
        return page - 2;
    }

    /**
     * Returns the next page number.
     *
     * @return the next page number
     */
    public int getNextPage() {
        return page + 1;
    }

    /**
     * Returns the page number after the previous page.
     *
     * @return the page number after the previous page
     */
    public int getPageAfterPrevious() {
        return page + 2;
    }
}
