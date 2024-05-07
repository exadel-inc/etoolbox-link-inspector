package com.exadel.etoolbox.linkinspector.core.servlets;

import com.exadel.etoolbox.linkinspector.core.services.util.BreadthTraverser;
import com.exadel.etoolbox.linkinspector.core.services.util.SlingQueryTraverser;
import com.exadel.etoolbox.linkinspector.core.services.util.StandardResourceTraverser;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;

import javax.servlet.Servlet;
import java.io.IOException;

@Component(service = {Servlet.class})
@SlingServletPaths(value = "/bin/etoolbox/link-inspector/node-traversing")
public class NodeTraversingServlet extends SlingSafeMethodsServlet {

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String path = request.getParameter("path");
        Resource resource = request.getResourceResolver().getResource(path);
        if (resource == null) {
            response.setStatus(404);
            return;
        }

        BreadthTraverser breadthTraverser = new BreadthTraverser();
        long start = System.currentTimeMillis();
        breadthTraverser.traverse(resource);
        int counter = breadthTraverser.getCounter();
        response.getWriter().write(String.format("BreadthTraverser found %d nodes in %d ms\n", counter, (System.currentTimeMillis() - start)));

        StandardResourceTraverser standardResourceTraverser = new StandardResourceTraverser();
        start = System.currentTimeMillis();
        standardResourceTraverser.accept(resource);
        counter = standardResourceTraverser.getCounter();
        response.getWriter().write(String.format("StandardResourceTraverser found %d nodes in %d ms\n", counter, (System.currentTimeMillis() - start)));

        SlingQueryTraverser slingQueryTraverser = new SlingQueryTraverser();
        start = System.currentTimeMillis();
        slingQueryTraverser.traverse(resource);
        counter = slingQueryTraverser.getCounter();
        response.getWriter().write(String.format("SlingQueryTraverser found %d nodes in %d ms\n", counter, (System.currentTimeMillis() - start)));

    }
}
