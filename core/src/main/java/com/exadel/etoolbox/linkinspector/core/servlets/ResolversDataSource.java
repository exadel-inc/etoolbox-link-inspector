package com.exadel.etoolbox.linkinspector.core.servlets;

import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.day.cq.commons.jcr.JcrConstants;
import com.exadel.etoolbox.linkinspector.api.Resolver;
import com.exadel.etoolbox.linkinspector.core.services.util.GraniteUtil;
import com.exadel.etoolbox.linkinspector.core.services.util.OcdUtil;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.MetaTypeService;

import javax.servlet.Servlet;
import java.util.*;

@Component(service = Servlet.class)
@SlingServletResourceTypes(resourceTypes = {
        "/bin/etoolbox/link-inspector/settings/resolvers",
        "/bin/etoolbox/link-inspector/settings/resolvers/cb"
})
public class ResolversDataSource extends SlingSafeMethodsServlet {

    private static final String PROP_TEXT = "text";
    private static final String SELECTED_PARAM = "selected";

    @Reference
    private transient MetaTypeService metaTypeService;

    @Reference(
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY
    )
    private transient volatile List<Resolver> linkResolvers;

    @Override
    protected void doGet(
            @NonNull SlingHttpServletRequest request,
            @NonNull SlingHttpServletResponse response) {

        String type = StringUtils.substringBefore(StringUtils.substringAfter(request.getHeader("Referer"), "type="), "&");

        List<Resource> resolverFields = new ArrayList<>();
        boolean isItemDataSource = StringUtils.endsWith(request.getResource().getResourceType(), "/resolvers");
        for (Resolver linkResolver : linkResolvers) {
            Resource item = isItemDataSource
                    ? createListItem(request, linkResolver, type)
                    : createCheckBox(request, linkResolver);
            if (item != null) {
                resolverFields.add(item);
            }
        }
        resolverFields.sort((o1, o2) -> StringUtils.compare(
                o1.getValueMap().get(PROP_TEXT, String.class),
                o2.getValueMap().get(PROP_TEXT, String.class)));
        DataSource dataSource = new SimpleDataSource(resolverFields.iterator());
        request.setAttribute(DataSource.class.getName(), dataSource);
    }

    private Resource createCheckBox(SlingHttpServletRequest request, Resolver linkResolver) {
        Map<String, Object> fieldProperties = new HashMap<>();
        fieldProperties.put(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, "granite/ui/components/coral/foundation/form/checkbox");
        String serviceName = linkResolver.getClass().getName();
        fieldProperties.put("name", "./" + serviceName + "/enabled");
        fieldProperties.put(PROP_TEXT, OcdUtil.getLabel(linkResolver, metaTypeService));
        fieldProperties.put("fieldDescription", StringUtils.substringAfterLast(serviceName, "."));
        fieldProperties.put("value", Boolean.TRUE);
        fieldProperties.put("uncheckedValue", Boolean.FALSE);
        fieldProperties.put("checked", Boolean.TRUE);
        return GraniteUtil.createResource(
                request.getResourceResolver(),
                serviceName,
                fieldProperties,
                Collections.emptyList());
    }

    private Resource createListItem(SlingHttpServletRequest request, Resolver linkResolver, String type) {
        if (!linkResolver.isEnabled()) {
            return null;
        }
        ValueMap valueMap = new ValueMapDecorator(new HashMap<>());
        String id = linkResolver.getId();
        valueMap.put("value", id);
        if (id.equalsIgnoreCase(type)) {
            valueMap.put(SELECTED_PARAM, true);
        }
        valueMap.put(PROP_TEXT, id);
        return new ValueMapResource(
                request.getResourceResolver(),
                StringUtils.EMPTY,
                JcrConstants.NT_UNSTRUCTURED,
                valueMap);
    }
}