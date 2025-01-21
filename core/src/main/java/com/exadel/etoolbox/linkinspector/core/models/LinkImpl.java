package com.exadel.etoolbox.linkinspector.core.models;

import com.exadel.etoolbox.linkinspector.api.Link;
import com.exadel.etoolbox.linkinspector.api.LinkStatus;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class LinkImpl implements Link {

    public static final String DEFAULT_TYPE = "other";

    private final String type;
    private final String href;
    private LinkStatus status;

    public LinkImpl(String type, String href) {
        this(type, href, LinkStatus.OK);
    }

    public LinkImpl(String type, String href, LinkStatus status) {
        this.type = type;
        this.href = href;
        this.status = status;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getHref() {
        return href;
    }

    @Override
    public String getMatchedText() {
        return href;
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
        LinkImpl link = (LinkImpl) o;
        return new EqualsBuilder().append(type, link.type).append(href, link.href).append(status, link.status).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(type).append(href).append(status).toHashCode();
    }
}
