package com.exadel.aembox.linkchecker.core.services.util;

import com.exadel.aembox.linkchecker.core.models.Link;

public class LinksCounter {
    private int internalLinks;
    private int externalLinks;

    public int getInternalLinks() {
        return internalLinks;
    }

    public int getExternalLinks() {
        return externalLinks;
    }

    public void incrementInternal() {
        this.internalLinks++;
    }

    public void incrementExternal() {
        this.externalLinks++;
    }

    public synchronized void countValidatedLinks(Link link) {
        switch (link.getType()) {
            case INTERNAL: {
                this.incrementInternal();
                break;
            }
            case EXTERNAL: {
                this.incrementExternal();
            }
        }
    }
}