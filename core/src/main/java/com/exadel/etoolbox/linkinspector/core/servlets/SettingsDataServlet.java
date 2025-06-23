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

import com.exadel.etoolbox.linkinspector.api.Resolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import javax.servlet.Servlet;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p><u>Note</u>: This class is not a part of the public API and is subject to change. Do not use it in your own code</p>
 * Provides configuration settings data for Link Inspector resolvers.
 * <p>
 * This servlet exposes an HTTP GET endpoint that retrieves and returns the current
 * configuration settings for all active link resolvers in the system. It uses the
 * OSGi ConfigurationAdmin service to access resolver configurations and formats them
 * as a JSON object.
 * <p>
 * The servlet is registered at the path "/bin/etoolbox/link-inspector/settings.json"
 * and is used by the Link Inspector UI to display and edit resolver settings.
 */
@Component(service = {Servlet.class})
@SlingServletResourceTypes(
        resourceTypes = "/bin/etoolbox/link-inspector/settings",
        extensions = "json",
        methods = HttpConstants.METHOD_GET)
@Slf4j
public class SettingsDataServlet extends SlingSafeMethodsServlet {

    @Reference
    private transient ConfigurationAdmin configurationAdmin;

    @Reference(
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY
    )
    private transient volatile List<Resolver> resolvers;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        if (CollectionUtils.isEmpty(resolvers)) {
            log.warn("No link resolvers found");
            response.getWriter().print("{}");
            response.getWriter().flush();
            return;
        }
        Map<String, Object> data = new HashMap<>();
        for (Resolver resolver : resolvers) {
            String pid = resolver.getClass().getName();
            Configuration configuration = configurationAdmin.getConfiguration(pid, null);
            if (configuration == null || configuration.getProperties() == null) {
                log.warn("Configuration for resolver {} not found", pid);
                continue;
            }
            Enumeration<String> keys = configuration.getProperties().keys();
            while (keys.hasMoreElements()) {
                String nextKey = keys.nextElement();
                if (StringUtils.isEmpty(nextKey) || "service.pid".equals(nextKey)) {
                    continue;
                }
                Object value = configuration.getProperties().get(nextKey);
                if (value != null) {
                    data.put("./" + pid + "/" + nextKey, value);
                }
            }
        }
        response.getWriter().print(new ObjectMapper().writeValueAsString(data));
        response.getWriter().flush();
    }
}
