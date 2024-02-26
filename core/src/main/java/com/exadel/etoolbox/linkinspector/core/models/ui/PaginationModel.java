package com.exadel.etoolbox.linkinspector.core.models.ui;

import com.exadel.etoolbox.linkinspector.core.services.data.impl.DataFeedServiceImpl;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import javax.annotation.PostConstruct;
import java.util.Optional;

@Model(
        adaptables = SlingHttpServletRequest.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class PaginationModel {
    private static final String REQUEST_PARAMETER_PAGE = "page";
    private static final String REPORTS_NODE_PROPERTY_SIZE = "size";
    private static final int DEFAULT_PAGE_NUMBER = 1;
    private static final int DEFAULT_NUMBER_OF_REPORTS = 0;

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
                .map(Integer::parseInt)
                .orElse(DEFAULT_PAGE_NUMBER);

        size = Optional
                .ofNullable(resourceResolver.getResource(DataFeedServiceImpl.CSV_REPORT_NODE_PATH))
                .map(Resource::getValueMap)
                .map(map -> map.get(REPORTS_NODE_PROPERTY_SIZE, Long.class))
                .map(Long::intValue)
                .orElse(DEFAULT_NUMBER_OF_REPORTS);
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public int getWithStepBack() {
        return decrement(page);
    }

    public int getWithTwoStepBack() {
        return decrement(decrement(page));
    }

    public int getWithStepForward() {
        return increment(page);
    }

    public int getWithTwoStepForward() {
        return increment(increment(page));
    }

    private int increment(int page) {
        return page + 1;
    }

    private int decrement(int page) {
        return page - 1;
    }
}
