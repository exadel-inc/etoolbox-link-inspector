package com.exadel.linkchecker.core.models;

public final class LinkStatus {
    private int statusCode;
    private String statusMessage;

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
