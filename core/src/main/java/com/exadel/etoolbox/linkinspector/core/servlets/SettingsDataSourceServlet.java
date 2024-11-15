package com.exadel.etoolbox.linkinspector.core.servlets;


import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.exadel.etoolbox.linkinspector.api.LinkResolver;
import com.exadel.etoolbox.linkinspector.core.services.util.GraniteUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;

import javax.servlet.Servlet;
import java.util.*;

@Component(service = {Servlet.class})
@SlingServletResourceTypes(
        resourceTypes = "/bin/etoolbox/link-inspector/settings",
        methods = HttpConstants.METHOD_GET)
public class SettingsDataSourceServlet extends SlingSafeMethodsServlet {

    @Reference
    private transient MetaTypeService metaTypeService;

    @Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    private transient volatile List<LinkResolver> linkResolvers;

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response) {

        String rootPath = request.getRequestPathInfo().getResourcePath();
        Resource mainTab = request.getResourceResolver().getResource(rootPath + "/main");
        Resource advancedTab = request.getResourceResolver().getResource(rootPath + "/advanced");

        List<Resource> tabs = new ArrayList<>();
        tabs.add(mainTab);
        tabs.add(advancedTab);

        Map<String, List<AttributeDefinition>> serviceSettings = getServiceSettings();
        for (Map.Entry<String, List<AttributeDefinition>> stringListEntry : serviceSettings.entrySet()) {
            String serviceName = stringListEntry.getKey();
            List<Resource> innerFields = new ArrayList<>();
            for (AttributeDefinition attributeDefinition : stringListEntry.getValue()) {
                if (attributeDefinition.getID().equals("linkType")) {
                    continue;
                }
                Map<String, Object> fieldProperties = new HashMap<>();
                fieldProperties.put(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, getResourceTypeByType(attributeDefinition.getType()));

                fieldProperties.put("name", "./" + serviceName + "/" + attributeDefinition.getID());
                fieldProperties.put("fieldLabel", attributeDefinition.getName());
                fieldProperties.put("fieldDescription", attributeDefinition.getDescription());
                Resource textField = GraniteUtil.createResource(request.getResourceResolver(), serviceName + "." + attributeDefinition.getID(), fieldProperties, Collections.emptyList());
                innerFields.add(textField);
            }
            Resource tab = GraniteUtil.createTab(request.getResourceResolver(), serviceName, splitCamelCase(StringUtils.substringAfterLast(serviceName, ".")), innerFields);
            tabs.add(tab);
        }

        DataSource dataSource = new SimpleDataSource(tabs.iterator());
        request.setAttribute(DataSource.class.getName(), dataSource);
    }

    private String splitCamelCase(String s) {
        return s.replaceAll(
                String.format("%s|%s|%s",
                        "(?<=[A-Z])(?=[A-Z][a-z])",
                        "(?<=[^A-Z])(?=[A-Z])",
                        "(?<=[A-Za-z])(?=[^A-Za-z])"
                ),
                " "
        );
    }

    private String getResourceTypeByType(int type) {
        String result;
        switch (type) {
            case 3:
                result = "granite/ui/components/coral/foundation/form/numberfield";
                break;
            default:
                result = "granite/ui/components/coral/foundation/form/textfield";
                break;
        }
        return result;
    }

    private Map<String, List<AttributeDefinition>> getServiceSettings() {
        Map<String, List<AttributeDefinition>> result = new HashMap<>();
        for (LinkResolver linkResolver : CollectionUtils.emptyIfNull(linkResolvers)) {
            Bundle bundle = FrameworkUtil.getBundle(linkResolver.getClass());
            MetaTypeInformation metaTypeInformation = metaTypeService.getMetaTypeInformation(bundle);
            ObjectClassDefinition objectClassDefinition = metaTypeInformation.getObjectClassDefinition(linkResolver.getClass().getName(), null);
            AttributeDefinition[] definitions = objectClassDefinition.getAttributeDefinitions(ObjectClassDefinition.ALL);

            result.put(linkResolver.getClass().getName(), Arrays.asList(definitions));
        }
        return result;
    }


}
