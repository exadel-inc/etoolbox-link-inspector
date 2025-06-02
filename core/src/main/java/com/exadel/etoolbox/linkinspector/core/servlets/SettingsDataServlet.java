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
