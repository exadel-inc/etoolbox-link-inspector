package com.exadel.linkchecker.core.services.impl;

import com.day.cq.rewriter.linkchecker.ExternalLinkChecker;
import com.exadel.linkchecker.core.models.Link;
import com.exadel.linkchecker.core.models.LinkStatus;
import com.exadel.linkchecker.core.services.LinkHelper;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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

    private static final Pattern PATTERN_EXTERNAL_LINK = Pattern.compile("(https?://(?:www\\.|(?!www))[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\\.[^\\s\"<]{2,}|www\\.[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\\.[^\\s\"<]{2,}|https?://(?:www\\.|(?!www))[a-zA-Z0-9]+\\.[^\\s\"<]{2,}|www\\.[a-zA-Z0-9]+\\.[^\\s\"<]{2,})");
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

    public Stream<String> getExternalLinksFromString(String text) {
        return getLinksByPattern(text, PATTERN_EXTERNAL_LINK);
    }

    public Stream<String> getInternalLinksFromString(String text) {
        return getLinksByPattern(text, PATTERN_INTERNAL_LINK);
    }

    public LinkStatus validateInternalLink(String link, ResourceResolver resourceResolver) {
        return Optional.ofNullable(resourceResolver.getResource(link))
                .map(resource -> new LinkStatus(HttpStatus.SC_OK, HttpStatus.getStatusText(HttpStatus.SC_OK)))
                .orElse(new LinkStatus(HttpStatus.SC_NOT_FOUND, HttpStatus.getStatusText(HttpStatus.SC_NOT_FOUND)));
    }

    public LinkStatus validateExternalLink(String link) {
        try {
            int statusCode = externalLinkChecker.check(link);
            String statusMessage = HttpStatus.getStatusText(statusCode);
            return new LinkStatus(statusCode, statusMessage);
        } catch (URISyntaxException | IOException e) {
            String message = Optional.ofNullable(e.getCause())
                    .map(Throwable::toString)
                    .orElse(e.toString());
            LOG.debug("Validation didn't pass for the external link {} due to the error: {}", link, message);
            return new LinkStatus(HttpStatus.SC_BAD_REQUEST, message);
        }
    }

    public boolean validateLink(Link link, ResourceResolver resourceResolver) {
        switch (link.getType()) {
            case INTERNAL: {
                link.setStatus(validateInternalLink(link.getHref(), resourceResolver));
                break;
            }
            case EXTERNAL: {
                link.setStatus(validateExternalLink(link.getHref()));
                LOG.trace("Validation of the external link {} completed", link.getHref());
            }
        }
        return link.isValid();
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
}
