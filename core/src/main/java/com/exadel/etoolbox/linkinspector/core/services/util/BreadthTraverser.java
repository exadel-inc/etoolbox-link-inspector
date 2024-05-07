package com.exadel.etoolbox.linkinspector.core.services.util;

import org.apache.sling.api.resource.Resource;

import java.util.LinkedList;
import java.util.Queue;

public class BreadthTraverser {

    private int counter;

    private Queue<Resource> queue = new LinkedList<>();

    public void traverse(Resource resource) {
        if (resource == null) {
            return;
        }
        resource.getChildren().forEach(queue::offer);
        counter++;
        while (!queue.isEmpty()) {
            Resource poll = queue.poll();
            if (poll != null) {
                poll.getChildren().forEach(queue::offer);
                counter++;
            }
        }
    }

    public int getCounter() {
        return counter;
    }
}
