package com.exadel.linkchecker.core.services.impl;

import com.exadel.linkchecker.core.models.Link;
import com.exadel.linkchecker.core.models.LinkStatus;
import com.exadel.linkchecker.core.services.ExternalLinkChecker;
import com.exadel.linkchecker.core.services.LinkHelper;
import com.exadel.linkchecker.core.services.util.constants.CommonConstants;
import org.apache.commons.httpclient.ConnectionPoolTimeoutException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Component(
        service = LinkHelper.class
)
public class LinkHelperImpl implements LinkHelper {
    private static final Logger LOG = LoggerFactory.getLogger(LinkHelper.class);

    private static final Pattern PATTERN_EXTERNAL_LINK = Pattern.compile("(https?://(?:www\\.|(?!www))[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\\.[^\\s\"'<]{2,}|www\\.[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\\.[^\\s\"'<]{2,}|https?://(?:www\\.|(?!www))[a-zA-Z0-9]+\\.[^\\s\"'<]{2,}|www\\.[a-zA-Z0-9]+\\.[^\\s\"'<]{2,})");
    private static final Pattern PATTERN_INTERNAL_LINK = Pattern.compile("(^|(?<=\"))/content/([-a-zA-Z0-9:%_+.~#?&/=\\s]*)");

    @Reference
    private ExternalLinkChecker externalLinkChecker;

    @Override
    public Stream<Link> getLinkStream(Object propertyValue) {
        Stream<Link> linkStream = Stream.empty();
        if (propertyValue instanceof String) {
            String stringValue = (String) propertyValue;
            linkStream = getLinksStream(stringValue);
        } else if (propertyValue instanceof String[]) {
            linkStream = Arrays.stream((String[]) propertyValue)
                    .flatMap(this::getLinksStream);
        }
        return linkStream;
    }

    @Override
    public Stream<String> getExternalLinksFromString(String text) {
        return getLinksByPattern(text, PATTERN_EXTERNAL_LINK);
    }

    @Override
    public Stream<String> getInternalLinksFromString(String text) {
        return getLinksByPattern(text, PATTERN_INTERNAL_LINK)
//                todo - exclude .html from internal links via regex
                .map(internalLink -> StringUtils.substringBefore(internalLink, CommonConstants.HTML_EXTENSION));
    }

    @Override
    public LinkStatus validateInternalLink(String link, ResourceResolver resourceResolver) {
        return Optional.ofNullable(resourceResolver.getResource(link))
                .map(resource -> new LinkStatus(HttpStatus.SC_OK, HttpStatus.getStatusText(HttpStatus.SC_OK)))
                .orElse(new LinkStatus(HttpStatus.SC_NOT_FOUND, HttpStatus.getStatusText(HttpStatus.SC_NOT_FOUND)));
    }

    @Override
    public LinkStatus validateExternalLink(String link) {
        try {
            int statusCode = externalLinkChecker.checkLink(link);
            String statusMessage = HttpStatus.getStatusText(statusCode);
            return new LinkStatus(statusCode, statusMessage);
        } catch (SocketTimeoutException | ConnectionPoolTimeoutException e) {
            String errorMessage = logValidationError(e, link);
            return new LinkStatus(HttpStatus.SC_REQUEST_TIMEOUT, errorMessage);
        } catch (URISyntaxException | IOException e) {
            String errorMessage = logValidationError(e, link);
            return new LinkStatus(HttpStatus.SC_BAD_REQUEST, errorMessage);
        }
    }

    @Override
    public LinkStatus validateLink(Link link, ResourceResolver resourceResolver) {
        switch (link.getType()) {
            case INTERNAL: {
                link.setStatus(validateInternalLink(link.getHref(), resourceResolver));
                break;
            }
            case EXTERNAL: {
                LOG.trace("Start validation of the external link {}", link.getHref());
                link.setStatus(validateExternalLink(link.getHref()));
                LOG.trace("Completed validation of the external link {}", link.getHref());
            }
        }
        return link.getStatus();
    }

    private Stream<Link> getLinksStream(String text) {
        Stream<Link> internalLinksStream = getInternalLinksFromString(text)
                .map(linkString -> new Link(linkString, Link.Type.INTERNAL))
                .distinct();
        Stream<Link> externalLinksStream = getExternalLinksFromString(text)
                .map(linkString -> new Link(linkString, Link.Type.EXTERNAL))
                .distinct();
        return Stream.concat(internalLinksStream, externalLinksStream);
    }

    private Stream<String> getLinksByPattern(String text, Pattern pattern) {
        Set<String> links = new HashSet<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String link = matcher.group();
            links.add(link);
        }
        return links.stream();
    }

    private String logValidationError(Exception e, String link) {
        String errorMessage = Optional.ofNullable(e.getCause())
                .map(Throwable::toString)
                .orElse(e.toString());
        LOG.debug("Validation didn't pass for the external link {} due to the error: {}", link, errorMessage);
        return errorMessage;
    }
}