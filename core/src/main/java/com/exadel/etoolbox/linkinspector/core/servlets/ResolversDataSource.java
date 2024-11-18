package com.exadel.etoolbox.linkinspector.core.servlets;

import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.day.cq.commons.jcr.JcrConstants;
import com.exadel.etoolbox.linkinspector.api.LinkResolver;
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
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.MetaTypeService;

import javax.servlet.Servlet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component(service = Servlet.class)
@SlingServletPaths({
        "/bin/etoolbox/link-inspector/settings/resolvers",
        "/bin/etoolbox/link-inspector/settings/resolvers/cb"
})
public class ResolversDataSource extends SlingSafeMethodsServlet {

    private static final String PROP_TEXT = "text";

    @Reference
    private transient MetaTypeService metaTypeService;

    @Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    private transient volatile List<LinkResolver> linkResolvers;

    @Override
    protected void doGet(
            @NonNull SlingHttpServletRequest request,
            @NonNull SlingHttpServletResponse response) {

        List<Resource> resolverFields = new ArrayList<>();
        boolean isItemDataSource = StringUtils.endsWith(request.getResource().getResourceType(), "/resolvers");
        for (LinkResolver linkResolver : linkResolvers) {
            Resource item = isItemDataSource
                    ? createListItem(request, linkResolver)
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

    private Resource createCheckBox(SlingHttpServletRequest request, LinkResolver linkResolver) {
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

    private Resource createListItem(SlingHttpServletRequest request, LinkResolver linkResolver) {
        if (!linkResolver.isEnabled()) {
            return null;
        }
        ValueMap valueMap = new ValueMapDecorator(new HashMap<>());
        valueMap.put("value", linkResolver.getId());
        valueMap.put(PROP_TEXT, linkResolver.getId());
        return new ValueMapResource(
                request.getResourceResolver(),
                StringUtils.EMPTY,
                JcrConstants.NT_UNSTRUCTURED,
                valueMap);
    }
}