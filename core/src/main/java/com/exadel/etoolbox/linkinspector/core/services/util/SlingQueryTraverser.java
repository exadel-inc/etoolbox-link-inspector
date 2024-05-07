package com.exadel.etoolbox.linkinspector.core.services.util;

import org.apache.sling.api.resource.Resource;

import static org.apache.sling.query.SlingQuery.$;

public class SlingQueryTraverser {

    private int counter;

    public void traverse(Resource resource) {
        if (resource == null) {
            return;
        }
        counter++;
        $(resource).children().forEach(this::traverse);
    }

    public int getCounter() {
        return counter;
    }
}
