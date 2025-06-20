package com.exadel.etoolbox.linkinspector.core.models;

import com.exadel.etoolbox.linkinspector.api.Result;
import com.exadel.etoolbox.linkinspector.api.Status;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

/**
 * Implementation of the Result interface that represents a link checking result.
 * Contains information about the link type, value, and validation status.
 */
@Getter
@EqualsAndHashCode
public class LinkResult implements Result {

    public static final String DEFAULT_TYPE = "other";

    private final String type;
    private final String value;
    @Setter
    private Status status;

    /**
     * Creates a new LinkResult with the specified type and value, with a default OK status
     *
     * @param type The type of link
     * @param value The link value
     */
    public LinkResult(String type, String value) {
        this(type, value, Status.OK);
    }

    /**
     * Creates a new LinkResult with the specified type, value and status
     *
     * @param type The type of link
     * @param value The link value
     * @param status The status of the link check
     */
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
