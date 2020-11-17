package com.exadel.linkchecker.core.models;

import com.google.common.collect.ImmutableList;
import org.apache.commons.httpclient.HttpStatus;

import java.util.List;

public final class LinkStatus {
    public static final List<Integer> HTTP_CODES_SUCCESS = ImmutableList.of(
            HttpStatus.SC_OK,
            HttpStatus.SC_CREATED,
            HttpStatus.SC_ACCEPTED,
            HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION,
            HttpStatus.SC_NO_CONTENT,
            HttpStatus.SC_RESET_CONTENT,
            HttpStatus.SC_PARTIAL_CONTENT,
            HttpStatus.SC_MULTI_STATUS
    );

    private final int statusCode;
    private final String statusMessage;
    private final boolean isValid;

    public LinkStatus(int statusCode, String statusMessage) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.isValid = HTTP_CODES_SUCCESS.contains(statusCode);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public boolean isValid() {
        return isValid;
    }
}
