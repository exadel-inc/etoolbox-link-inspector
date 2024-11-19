package com.exadel.etoolbox.linkinspector.core.servlets;

import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.exadel.etoolbox.linkinspector.api.LinkResolver;
import com.exadel.etoolbox.linkinspector.core.services.util.GraniteUtil;
import com.exadel.etoolbox.linkinspector.core.services.util.OcdUtil;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
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
import org.osgi.service.component.annotations.ReferenceCardinality;
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
public class SettingsDataSource extends SlingSafeMethodsServlet {

    private static final String RESTYPE_TEXT_FIELD = "granite/ui/components/coral/foundation/form/textfield";
    private static final String RESTYPE_NUMBER_FIELD = "granite/ui/components/coral/foundation/form/numberfield";

    @Reference
    private transient MetaTypeService metaTypeService;

    @Reference(
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY
    )
    private transient volatile List<LinkResolver> linkResolvers;

    @Override
    protected void doGet(@NonNull SlingHttpServletRequest request, @NonNull SlingHttpServletResponse response) {
        String rootPath = request.getRequestPathInfo().getResourcePath();
        Resource mainTab = request.getResourceResolver().getResource(rootPath + "/main");
        Resource advancedTab = request.getResourceResolver().getResource(rootPath + "/advanced");

        List<Resource> tabs = new ArrayList<>();
        tabs.add(mainTab);
        tabs.add(advancedTab);

        for (ServiceConfig serviceConfig : getServiceConfigs()) {
            List<Resource> innerFields = new ArrayList<>();
            for (AttributeDefinition attributeDefinition : serviceConfig.getDefinitions()) {
                if (attributeDefinition.getID().equals("enabled")) {
                    continue;
                }
                Map<String, Object> fieldProperties = new HashMap<>();
                String resourceType = getResourceType(attributeDefinition.getType());
                fieldProperties.put(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, resourceType);
                fieldProperties.put("name", "./" + serviceConfig.getId() + "/" + attributeDefinition.getID());
                fieldProperties.put("fieldLabel", attributeDefinition.getName());
                fieldProperties.put("fieldDescription", attributeDefinition.getDescription());

                if (ArrayUtils.getLength(attributeDefinition.getDefaultValue()) == 1) {
                    if (RESTYPE_TEXT_FIELD.equals(resourceType)) {
                        fieldProperties.put("emptyText", attributeDefinition.getDefaultValue()[0]);
                    } else if (RESTYPE_NUMBER_FIELD.equals(resourceType)) {
                        fieldProperties.put("value", attributeDefinition.getDefaultValue()[0]);
                    }
                }
                Resource field = GraniteUtil.createResource(
                        request.getResourceResolver(),
                        serviceConfig.getId() + "." + attributeDefinition.getID(),
                        fieldProperties, Collections.emptyList());

                innerFields.add(field);
            }
            if (innerFields.isEmpty()) {
                continue;
            }
            Resource tab = GraniteUtil.createTab(request.getResourceResolver(), serviceConfig.getId(), serviceConfig.getLabel(), innerFields);
            tabs.add(tab);
        }

        DataSource dataSource = new SimpleDataSource(tabs.iterator());
        request.setAttribute(DataSource.class.getName(), dataSource);
    }

    private List<ServiceConfig> getServiceConfigs() {
        List<ServiceConfig> result = new ArrayList<>();
        for (LinkResolver linkResolver : CollectionUtils.emptyIfNull(linkResolvers)) {
            Bundle bundle = FrameworkUtil.getBundle(linkResolver.getClass());
            MetaTypeInformation metaTypeInformation = metaTypeService.getMetaTypeInformation(bundle);
            ObjectClassDefinition objectClassDefinition = metaTypeInformation.getObjectClassDefinition(linkResolver.getClass().getName(), null);
            AttributeDefinition[] definitions = objectClassDefinition.getAttributeDefinitions(ObjectClassDefinition.ALL);

            String id = linkResolver.getClass().getName();
            String label = OcdUtil.getLabel(linkResolver, metaTypeService);
            result.add(new ServiceConfig(id, label, definitions));
        }
        result.sort(Comparator.comparing(ServiceConfig::getLabel));
        return result;
    }

    private static String getResourceType(int type) {
        if (type == 3) {
            return RESTYPE_NUMBER_FIELD;
        }
        return RESTYPE_TEXT_FIELD;
    }

    @RequiredArgsConstructor
    @Getter
    private static class ServiceConfig {
        private final String id;
        private final String label;
        private final AttributeDefinition[] definitions;
    }
}
