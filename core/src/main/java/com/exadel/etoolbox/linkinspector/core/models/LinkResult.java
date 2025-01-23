package com.exadel.etoolbox.linkinspector.core.models;

import com.exadel.etoolbox.linkinspector.api.Result;
import com.exadel.etoolbox.linkinspector.api.LinkStatus;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class LinkResult implements Result {

    public static final String DEFAULT_TYPE = "other";

    private final String type;
    private final String value;
    private LinkStatus status;

    public LinkResult(String type, String value) {
        this(type, value, LinkStatus.OK);
    }

    public LinkResult(String type, String value, LinkStatus status) {
        this.type = type;
        this.value = value;
        this.status = status;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getMatch() {
        return StringUtils.EMPTY;
    }

    @Override
    public LinkStatus getStatus() {
        return status;
    }

    @Override
    public void setStatus(LinkStatus value) {
        this.status = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LinkResult link = (LinkResult) o;
        return new EqualsBuilder().append(type, link.type).append(value, link.value).append(status, link.status).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(type).append(value).append(status).toHashCode();
    }
}
