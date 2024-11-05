package com.exadel.etoolbox.linkinspector.core.servlets;

import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.exadel.etoolbox.linkinspector.api.LinkResolver;
import com.exadel.etoolbox.linkinspector.core.services.util.GraniteUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.*;

import javax.servlet.Servlet;
import java.util.*;

@Component(service = {Servlet.class})
@SlingServletResourceTypes(
        resourceTypes = "/bin/etoolbox/link-inspector/settings/linktypes",
        methods = HttpConstants.METHOD_GET)
public class LinkTypesDataSourceServlet extends SlingSafeMethodsServlet {

    @Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    private transient volatile List<LinkResolver> linkResolvers;

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response) {

        List<Resource> links = new ArrayList<>();
        for (LinkResolver linkResolver : linkResolvers) {
            Map<String, Object> fieldProperties = new HashMap<>();
            fieldProperties.put(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, "granite/ui/components/coral/foundation/form/checkbox");

            String serviceName = linkResolver.getClass().getName();
            fieldProperties.put("name", "./" + serviceName + "/linkType");
            fieldProperties.put("text", StringUtils.substringAfterLast(serviceName, "."));
            fieldProperties.put("fieldDescription", StringUtils.substringAfterLast(serviceName, "."));
            fieldProperties.put("uncheckedValue", Boolean.FALSE);
            fieldProperties.put("checked", Boolean.FALSE);
            fieldProperties.put("value", Boolean.TRUE);
            Resource checkBox = GraniteUtil.createResource(request.getResourceResolver(), serviceName, fieldProperties, Collections.emptyList());
            links.add(checkBox);
        }

        DataSource dataSource = new SimpleDataSource(links.iterator());
        request.setAttribute(DataSource.class.getName(), dataSource);
    }
}
