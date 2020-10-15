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

    private static final String REGEX_EXTERNAL_LINK = "(https?://(?:www\\.|(?!www))[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\\.[^\\s]{2,}|www\\.[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\\.[^\\s]{2,}|https?://(?:www\\.|(?!www))[a-zA-Z0-9]+\\.[^\\s]{2,}|www\\.[a-zA-Z0-9]+\\.[^\\s]{2,})";
    private static final String REGEX_INTERNAL_LINK = "(^|(?<=\"))\\/content\\/([-a-zA-Z0-9:%_\\+.~#?&//=]*)";

    private static final String LINK_TYPE_EXTERNAL = "External";
    private static final String LINK_TYPE_INTERNAL = "Internal";

    @Reference
    private ExternalLinkChecker externalLinkChecker;

    @Override
    public Stream<Link> getLinkStream(Object propertyValue, ResourceResolver resourceResolver) {
        Stream<Link> linkStream = Stream.empty();
        if (propertyValue instanceof String) {
            String stringValue = (String) propertyValue;
            linkStream = getLinksStream(stringValue, resourceResolver);
        } else if (propertyValue instanceof String[]) {
            linkStream = Arrays.stream((String[]) propertyValue)
                    .flatMap(stringValue -> getLinksStream(stringValue, resourceResolver));
        }
        return linkStream;
    }

    public Stream<String> getExternalLinksFromString(String text) {
        return getLinksByRegex(text, REGEX_EXTERNAL_LINK);
    }

    public Stream<String> getInternalLinksFromString(String text) {
        return getLinksByRegex(text, REGEX_INTERNAL_LINK);
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
            LOG.debug("Result of checking the link " + link + " " + message);
            return new LinkStatus(HttpStatus.SC_BAD_REQUEST, message);
        }
    }

    private Stream<Link> getLinksStream(String text, ResourceResolver resourceResolver) {
        Stream<Link> internalLinksStream = getInternalLinksFromString(text)
                .map(linkString -> new Link(linkString, LINK_TYPE_INTERNAL, validateInternalLink(linkString, resourceResolver)))
                .distinct();
        Stream<Link> externalLinksStream = getExternalLinksFromString(text)
                .map(linkString -> new Link(linkString, LINK_TYPE_EXTERNAL, validateExternalLink(linkString)))
                .distinct();
        return Stream.concat(internalLinksStream, externalLinksStream);
    }

    private Stream<String> getLinksByRegex(String text, String regex) {
        Set<String> links = new HashSet<>();
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String link = matcher.group();
            links.add(link);
        }
        return links.stream();
    }
}
