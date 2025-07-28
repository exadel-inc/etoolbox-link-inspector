/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exadel.etoolbox.linkinspector.core.servlets;

import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.exadel.etoolbox.linkinspector.api.Resolver;
import com.exadel.etoolbox.linkinspector.core.services.util.GraniteUtil;
import com.exadel.etoolbox.linkinspector.core.services.util.OcdUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;

import javax.servlet.Servlet;
import java.util.*;

/**
 * <p><u>Note</u>: This class is not a part of the public API and is subject to change. Do not use it in your own code</p>
 * Provides a DataSource for the Link Inspector settings UI.
 * <p>
 * This servlet dynamically generates UI components for configuring resolver settings
 * based on the available link resolvers and their OSGi metadata. It creates tabs and
 * form fields for each resolver with appropriate types (text fields, checkboxes, number fields)
 * based on the attribute definitions from the resolvers' OSGi metadata.
 * <p>
 * The servlet is registered at the path "/bin/etoolbox/link-inspector/settings" and
 * is used by the Link Inspector UI to build the configuration interface.
 */
@Component(service = {Servlet.class})
@SlingServletResourceTypes(
        resourceTypes = "/bin/etoolbox/link-inspector/settings",
        methods = HttpConstants.METHOD_GET)
@Slf4j
public class SettingsDataSource extends SlingSafeMethodsServlet {

    private static final String RESTYPE_CHECKBOX = "granite/ui/components/coral/foundation/form/checkbox";
    private static final String RESTYPE_TEXT_FIELD = "granite/ui/components/coral/foundation/form/textfield";
    private static final String RESTYPE_NUMBER_FIELD = "granite/ui/components/coral/foundation/form/numberfield";

    @Reference
    private transient MetaTypeService metaTypeService;

    @Reference(
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY
    )
    private transient volatile List<Resolver> linkResolvers;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        String rootPath = request.getRequestPathInfo().getResourcePath();
        Resource mainTab = request.getResourceResolver().getResource(rootPath + "/main");
        Resource advancedTab = request.getResourceResolver().getResource(rootPath + "/advanced");

        List<Resource> tabs = new ArrayList<>();
        tabs.add(mainTab);
        tabs.add(advancedTab);

        for (ConfigDefinition configDefinition : getConfigDefinitions()) {
            List<Resource> innerFields = new ArrayList<>();
            for (AttributeDefinition attributeDefinition : configDefinition.getDefinitions()) {
                if (attributeDefinition.getID().equals("enabled")) {
                    continue;
                }
                Map<String, Object> fieldProperties = new HashMap<>();
                String resourceType = getResourceType(attributeDefinition.getType());
                // Generic properties
                fieldProperties.put(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, resourceType);
                fieldProperties.put("name", "./" + configDefinition.getId() + "/" + attributeDefinition.getID());
                fieldProperties.put("fieldDescription", attributeDefinition.getDescription());
                // Labels
                if (RESTYPE_CHECKBOX.equals(resourceType)) {
                    fieldProperties.put("text", attributeDefinition.getName());
                } else {
                    fieldProperties.put("fieldLabel", attributeDefinition.getName());
                }
                // Value members
                if (RESTYPE_CHECKBOX.equals(resourceType)) {
                    fieldProperties.put("value", Boolean.TRUE.toString());
                    fieldProperties.put("uncheckedValue", Boolean.FALSE.toString());
                }
                // Default value
                if (ArrayUtils.getLength(attributeDefinition.getDefaultValue()) == 1) {
                    if (RESTYPE_TEXT_FIELD.equals(resourceType)) {
                        fieldProperties.put("emptyText", attributeDefinition.getDefaultValue()[0]);
                    } else if (RESTYPE_NUMBER_FIELD.equals(resourceType)) {
                        fieldProperties.put("value", attributeDefinition.getDefaultValue()[0]);
                    } else if (RESTYPE_CHECKBOX.equals(resourceType)) {
                        fieldProperties.put("checked", Boolean.parseBoolean(attributeDefinition.getDefaultValue()[0]));
                    }
                }
                Resource field = GraniteUtil.createResource(
                        request.getResourceResolver(),
                        configDefinition.getId() + "." + attributeDefinition.getID(),
                        fieldProperties, Collections.emptyList());

                innerFields.add(field);
            }
            if (innerFields.isEmpty()) {
                continue;
            }
            Resource tab = GraniteUtil.createTab(request.getResourceResolver(), configDefinition.getId(), configDefinition.getLabel(), innerFields);
            tabs.add(tab);
        }

        DataSource dataSource = new SimpleDataSource(tabs.iterator());
        request.setAttribute(DataSource.class.getName(), dataSource);
    }

    private List<ConfigDefinition> getConfigDefinitions() {
        List<ConfigDefinition> result = new ArrayList<>();
        for (Resolver linkResolver : CollectionUtils.emptyIfNull(linkResolvers)) {
            Bundle bundle = FrameworkUtil.getBundle(linkResolver.getClass());
            MetaTypeInformation metaTypeInformation = metaTypeService.getMetaTypeInformation(bundle);
            if (metaTypeInformation == null) {
                return result;
            }
            ObjectClassDefinition objectClassDefinition = null;
            try {
                objectClassDefinition = metaTypeInformation.getObjectClassDefinition(linkResolver.getClass().getName(), null);
            } catch (IllegalArgumentException e) {
                log.error("Unable to get ObjectClassDefinition for {}.", linkResolver.getClass().getName());
            }
            if (objectClassDefinition == null) {
                continue;
            }
            AttributeDefinition[] definitions = objectClassDefinition.getAttributeDefinitions(ObjectClassDefinition.ALL);

            String id = linkResolver.getClass().getName();
            String label = OcdUtil.getLabel(linkResolver, metaTypeService);
            result.add(new ConfigDefinition(id, label, definitions));
        }
        result.sort(Comparator.comparing(ConfigDefinition::getLabel));
        return result;
    }

    private static String getResourceType(int type) {
        if (type == AttributeDefinition.INTEGER || type == AttributeDefinition.LONG || type == AttributeDefinition.SHORT) {
            return RESTYPE_NUMBER_FIELD;
        }
        if (type == AttributeDefinition.BOOLEAN) {
            return RESTYPE_CHECKBOX;
        }
        return RESTYPE_TEXT_FIELD;
    }

    @RequiredArgsConstructor
    @Getter
    private static class ConfigDefinition {
        private final String id;
        private final String label;
        private final AttributeDefinition[] definitions;
    }
}
