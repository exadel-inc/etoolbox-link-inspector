/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exadel.etoolbox.linkinspector.core.services.helpers.impl;

import com.exadel.etoolbox.linkinspector.core.models.Link;
import com.exadel.etoolbox.linkinspector.core.models.LinkStatus;
import com.exadel.etoolbox.linkinspector.core.services.ExternalLinkChecker;
import com.exadel.etoolbox.linkinspector.core.services.helpers.LinkHelper;
import com.exadel.etoolbox.linkinspector.core.services.util.LinkInspectorResourceUtil;
import org.apache.commons.httpclient.ConnectionPoolTimeoutException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
    private static final Logger LOG = LoggerFactory.getLogger(LinkHelperImpl.class);

    private static final Pattern PATTERN_EXTERNAL_LINK = Pattern.compile("https?://[\\w\\d-]+\\.[^\\s\"'<]{2,}|www\\d*\\.[\\w\\d-]+\\.[^\\s\"'<]{2,}");
    private static final Pattern PATTERN_INTERNAL_LINK = Pattern.compile("(^|(?<=\"))/content/([-\\w\\d():%_+.~#?&/=\\s]*)");

    @Reference
    private ExternalLinkChecker externalLinkChecker;

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<Link> getLinkStreamFromProperty(Object propertyValue) {
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

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<String> getExternalLinksFromString(String text) {
        return getLinksStreamByPattern(text, PATTERN_EXTERNAL_LINK);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<String> getInternalLinksFromString(String text) {
        return getLinksStreamByPattern(text, PATTERN_INTERNAL_LINK);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LinkStatus validateInternalLink(String link, ResourceResolver resourceResolver) {
        LinkStatus status = resolveInternalLink(link, resourceResolver);
        if (!status.isValid()) {
            String decodedLink = decodeLink(link);
            if (!decodedLink.equals(link)) {
                status = resolveInternalLink(decodedLink, resourceResolver);
            }
        }
        return status;
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public LinkStatus validateLink(Link link, ResourceResolver resourceResolver) {
        if (link.getType() == Link.Type.INTERNAL) {
            link.setStatus(validateInternalLink(link.getHref(), resourceResolver));
        } else if (link.getType() == Link.Type.EXTERNAL) {
            LOG.trace("Start validation of the external link {}", link.getHref());
            link.setStatus(validateExternalLink(link.getHref()));
            LOG.trace("Completed validation of the external link {}", link.getHref());
        }
        return link.getStatus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LinkStatus validateLink(String link, ResourceResolver resourceResolver) {
        Optional<Link> detectedLink = getLinkStreamFromProperty(link)
                .findFirst();
        if (!detectedLink.isPresent() || !detectedLink.get().getHref().equals(link)) {
            return new LinkStatus(HttpStatus.SC_BAD_REQUEST, "The provided link doesn't fit internal nor external link patterns");
        }
        return validateLink(detectedLink.get(), resourceResolver);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean replaceLink(ResourceResolver resourceResolver,
                               String resourcePath, String propertyName, String currentLink, String newLink) {
        return Optional.of(resourcePath)
                .map(resourceResolver::getResource)
                .map(resource -> resource.adaptTo(ModifiableValueMap.class))
                .map(modifiableValueMap ->
                        updateValueMapWithNewLink(modifiableValueMap, propertyName, currentLink, newLink)
                )
                .orElse(false);
    }

    private LinkStatus resolveInternalLink(String link, ResourceResolver resourceResolver) {
        return Optional.of(resourceResolver.resolve(link))
                .filter(resource -> !ResourceUtil.isNonExistingResource(resource))
                .map(resource -> new LinkStatus(HttpStatus.SC_OK, HttpStatus.getStatusText(HttpStatus.SC_OK)))
                .orElse(new LinkStatus(HttpStatus.SC_NOT_FOUND, HttpStatus.getStatusText(HttpStatus.SC_NOT_FOUND)));
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

    private Stream<String> getLinksStreamByPattern(String text, Pattern pattern) {
        Set<String> links = new HashSet<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String link = matcher.group();
            links.add(link);
        }
        return links.stream();
    }

    private boolean updateValueMapWithNewLink(ModifiableValueMap modifiableValueMap,
                                              String propertyName, String currentLink, String newLink) {
        boolean updated = false;
        Optional<Object> updatedValue = Optional.ofNullable(modifiableValueMap.get(propertyName))
                .map(value -> updatePropertyWithNewLink(value, currentLink, newLink));
        if (updatedValue.isPresent()) {
            modifiableValueMap.put(propertyName, updatedValue.get());
            updated = true;
        }
        return updated;
    }

    private Object updatePropertyWithNewLink(Object value, String currentLink, String newLink) {
        return getLinkStreamFromProperty(value)
                .map(Link::getHref)
                .filter(currentLink::equals)
                .findFirst()
                .map(currentLinkToReplace ->
                        LinkInspectorResourceUtil.replaceStringInPropValue(value, currentLinkToReplace, newLink))
                .orElse(null);
    }

    private String decodeLink(String link) {
        try {
            return URLDecoder.decode(link, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            LOG.error("Failed to decode a link", e);
        }
        return link;
    }

    private String logValidationError(Exception e, String link) {
        String errorMessage = Optional.ofNullable(e.getCause())
                .map(Throwable::toString)
                .orElse(e.toString());
        LOG.debug("Validation didn't pass for the external link {} due to the error: {}", link, errorMessage);
        return errorMessage;
    }
}