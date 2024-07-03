package com.exadel.etoolbox.linkinspector.api;

import org.eclipse.jetty.http.HttpStatus;

public interface Link {

    String getType();

    String getHref();

    LinkStatus getStatus();

    default boolean isReported() {
        return !getStatus().isValid();
    }

    void setStatus(LinkStatus status);

    default void setStatus(int code) {
        setStatus(new LinkStatus(code, HttpStatus.getMessage(code)));
    }

    default void setStatus(String message) {
        if (getStatus() == null) {
            setStatus(new LinkStatus(0, message));
        }
        setStatus(new LinkStatus(getStatus().getCode(), message));
    }

    default void setStatus(int code, String message) {
        setStatus(new LinkStatus(code, message));
    }
}
