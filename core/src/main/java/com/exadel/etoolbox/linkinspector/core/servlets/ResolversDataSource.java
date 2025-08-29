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

/**
 * Provides DataSource for resolver-related UI components of the Link Inspector.
 * <p>
 * This servlet dynamically creates resources based on the available link resolvers,
 * exposing them as a DataSource for Granite UI components. It serves two types of resources:
 * <ul>
 *   <li>Resolver list items for selection dropdowns</li>
 *   <li>Resolver checkboxes for configuration forms</li>
 * </ul>
 * <p>
 * The servlet is registered at the paths "/bin/etoolbox/link-inspector/settings/resolvers"
 * and "/bin/etoolbox/link-inspector/settings/resolvers/cb" and is used by the Link Inspector UI
 * to populate resolver-related configuration fields.
 * <p><u>Note</u>: This class is not a part of the public API and is subject to change. Do not use it in your own code</p>
 */
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