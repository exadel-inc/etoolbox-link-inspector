package com.exadel.etoolbox.linkinspector.core.models.ui;

import com.exadel.etoolbox.linkinspector.core.services.data.DataFeedService;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Represents the model with pagination logic.
 */
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
                .filter(NumberUtils::isNumber)
                .map(Integer::parseInt)
                .orElse(DEFAULT_PAGE_NUMBER);

        String type = Optional
                .ofNullable(request.getRequestParameter("type"))
                .map(RequestParameter::getString)
                .orElse(StringUtils.EMPTY);

        if (StringUtils.isNotBlank(type)) {
            Map<String, String> map = new HashMap<>();
            map.put("external", "brokenExternalLinks");
            map.put("internal", "brokenInternalLinks");

            size = Optional
                    .ofNullable(resourceResolver.getResource("/content/etoolbox-link-inspector/data/stats"))
                    .map(Resource::getValueMap)
                    .map(valueMap -> map.get(map.get(type)))
                    .map(Integer::parseInt)
                    .map(sum -> (int) Math.ceil((double) sum/500))
                    .orElse(DEFAULT_NUMBER_OF_REPORTS);
        } else {
            size = Optional
                    .ofNullable(resourceResolver.getResource("/content/etoolbox-link-inspector/data/stats"))
                    .map(Resource::getValueMap)
                    .map(map -> Stream.of(map.get("brokenExternalLinks", Integer.class), map.get("brokenInternalLinks", Integer.class))
                            .filter(Objects::nonNull)
                            .reduce(0, Integer::sum))
                    .map(sum -> (int) Math.ceil((double) sum/500))
                    .orElse(DEFAULT_NUMBER_OF_REPORTS);
        }
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
