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

package com.exadel.etoolbox.linkinspector.core.services.resolvers;

import com.exadel.etoolbox.linkinspector.api.Link;
import com.exadel.etoolbox.linkinspector.api.LinkResolver;
import com.exadel.etoolbox.linkinspector.api.LinkStatus;
import com.exadel.etoolbox.linkinspector.core.models.LinkImpl;
import com.exadel.etoolbox.linkinspector.core.services.data.UserConfig;
import com.exadel.etoolbox.linkinspector.core.services.resolvers.configs.InternalLinkResolverConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates external links via sending HEAD requests concurrently using {@link PoolingHttpClientConnectionManager}
 */
@Component(service = LinkResolver.class )
@Designate(ocd = InternalLinkResolverConfig.class)
public class InternalLinkResolverImpl implements LinkResolver {

    private static final Logger LOG = LoggerFactory.getLogger(InternalLinkResolverImpl.class);

    private static final Pattern PATTERN_INTERNAL_LINK = Pattern.compile("(^|(?<=\"))/content/([-\\w\\d():%_+.~#?&/=\\s]*)", Pattern.UNICODE_CHARACTER_CLASS);

    private String internalLinksHost;
    private boolean enabled;

    @Reference
    private LinkResolver externalLinkResolver;

    @Reference
    private UserConfig userConfig;

    @Activate
    @Modified
    private void activate(InternalLinkResolverConfig config){
        config = userConfig.apply(config, this.getClass());
        this.enabled = config.enabled();
        this.internalLinksHost = config.internalLinksHost();
    }


    @Override
    public String getId() {
        return "Internal";
    }

    @Override
    public Collection<Link> getLinks(String source) {
        if (!enabled) {
            return Collections.emptyList();
        }
        Set<Link> links = new HashSet<>();
        Matcher matcher = PATTERN_INTERNAL_LINK.matcher(source);
        while (matcher.find()) {
            String href = matcher.group();
            links.add(new LinkImpl(getId(), href));
        }
        return links;
    }

    @Override
    public void validate(Link link, ResourceResolver resourceResolver) {
        if (link == null || !StringUtils.equalsIgnoreCase(getId(), link.getType())) {
            return;
        }
        LinkStatus status = checkLink(link.getHref(), resourceResolver);
        if (status.getCode() == HttpStatus.SC_NOT_FOUND && StringUtils.isNotBlank(internalLinksHost)) {
            externalLinkResolver.validate(link, resourceResolver);
        } else {
            link.setStatus(status.getCode(), status.getMessage());
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    private LinkStatus checkLink(String href, ResourceResolver resourceResolver) {
        LinkStatus status = checkLinkInternal(href, resourceResolver);
        if (!status.isValid()) {
            String decodedLink = decode(href);
            if (!decodedLink.equals(href)) {
                status = checkLinkInternal(decodedLink, resourceResolver);
            }
        }
        return status;
    }

    private LinkStatus checkLinkInternal(String href, ResourceResolver resourceResolver) {
        return Optional.of(resourceResolver.resolve(href))
                .filter(resource -> !ResourceUtil.isNonExistingResource(resource))
                .map(resource -> new LinkStatus(HttpStatus.SC_OK, "OK"))
                .orElse(new LinkStatus(HttpStatus.SC_NOT_FOUND, "Not Found"));
    }

    private String decode(String href) {
        try {
            return URLDecoder.decode(href, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            LOG.error("Failed to decode a link", e);
        }
        return href;
    }
}