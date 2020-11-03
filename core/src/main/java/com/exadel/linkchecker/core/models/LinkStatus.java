package com.exadel.linkchecker.core.models;

public final class LinkStatus {
    private final int statusCode;
    private final String statusMessage;

    public LinkStatus(int statusCode, String statusMessage) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }
}
