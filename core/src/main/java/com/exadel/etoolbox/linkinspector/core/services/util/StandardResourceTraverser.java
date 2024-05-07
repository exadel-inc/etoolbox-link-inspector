package com.exadel.etoolbox.linkinspector.core.services.util;

import org.apache.sling.api.resource.AbstractResourceVisitor;
import org.apache.sling.api.resource.Resource;

public class StandardResourceTraverser extends AbstractResourceVisitor {

    private int counter;

    @Override
    protected void visit(Resource resource) {
        counter++;
    }

    public int getCounter() {
        return counter;
    }
}
