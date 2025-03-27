package com.exadel.etoolbox.linkinspector.core.models;

import com.exadel.etoolbox.linkinspector.api.Result;
import com.exadel.etoolbox.linkinspector.api.Status;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

@Getter
@EqualsAndHashCode
public class LinkResult implements Result {

    public static final String DEFAULT_TYPE = "other";

    private final String type;
    private final String value;
    @Setter
    private Status status;

    public LinkResult(String type, String value) {
        this(type, value, Status.OK);
    }

    public LinkResult(String type, String value, Status status) {
        this.type = type;
        this.value = value;
        this.status = status;
    }

    @Override
    public String getMatch() {
        return StringUtils.EMPTY;
    }
}
