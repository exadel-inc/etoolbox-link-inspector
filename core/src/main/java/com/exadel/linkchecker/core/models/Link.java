package com.exadel.linkchecker.core.models;

import org.apache.commons.httpclient.HttpStatus;

import java.util.Objects;

public final class Link {
    private final String href;
    private final String type;
    private final boolean isValid;
    private final int statusCode;
    private final String statusMessage;

    public Link(String href, String type, LinkStatus status) {
        this.href = href;
        this.type = type;
        this.statusCode = status.getStatusCode();
        this.statusMessage = status.getStatusMessage();
        this.isValid = this.statusCode == HttpStatus.SC_OK;
    }

    public String getHref() {
        return href;
    }

    public String getType() {
        return type;
    }

    public boolean isValid() {
        return isValid;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Link link = (Link) o;
        return href.equals(link.href) &&
                type.equals(link.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(href, type);
    }
}
