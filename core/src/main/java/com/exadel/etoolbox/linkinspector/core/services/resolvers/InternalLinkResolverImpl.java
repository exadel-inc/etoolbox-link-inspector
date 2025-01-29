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

import com.exadel.etoolbox.linkinspector.api.Result;
import com.exadel.etoolbox.linkinspector.api.Resolver;
import com.exadel.etoolbox.linkinspector.api.Status;
import com.exadel.etoolbox.linkinspector.core.models.LinkResult;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates external links via sending HEAD requests concurrently using {@link PoolingHttpClientConnectionManager}
 */
@Component(service = Resolver.class, immediate = true)
@Designate(ocd = InternalLinkResolverConfig.class)
public class InternalLinkResolverImpl implements Resolver {

    private static final Logger LOG = LoggerFactory.getLogger(InternalLinkResolverImpl.class);

    private static final Pattern PATTERN_INTERNAL_LINK = Pattern.compile("(^|(?<=\"))/content/([-\\w\\d():%_+.~#?&/=\\s]*)", Pattern.UNICODE_CHARACTER_CLASS);

    private String internalLinksHost;
    private boolean enabled;

    @Reference
    private Resolver externalLinkResolver;

    @Activate
    @Modified
    private void activate(InternalLinkResolverConfig config) {
        this.enabled = config.enabled();
        this.internalLinksHost = config.internalLinksHost();
    }

    @Override
    public String getId() {
        return "Internal";
    }

    @Override
    public Collection<Result> getLinks(String source) {
        if (!enabled) {
            return Collections.emptyList();
        }
        Set<Result> results = new HashSet<>();
        Matcher matcher = PATTERN_INTERNAL_LINK.matcher(source);
        while (matcher.find()) {
            String href = matcher.group();
            results.add(new LinkResult(getId(), href));
        }
        return results;
    }

    @Override
    public void validate(Result result, ResourceResolver resourceResolver) {
        if (result == null || !StringUtils.equalsIgnoreCase(getId(), result.getType())) {
            return;
        }
        Status status = checkLink(result.getValue(), resourceResolver);
        if (status.getCode() == HttpStatus.SC_NOT_FOUND && StringUtils.isNotBlank(internalLinksHost)) {
            externalLinkResolver.validate(result, resourceResolver);
        } else {
            result.setStatus(status.getCode(), status.getMessage());
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    private Status checkLink(String href, ResourceResolver resourceResolver) {
        Status status = checkLinkInternal(href, resourceResolver);
        if (!status.isValid()) {
            String decodedLink = decode(href);
            if (!decodedLink.equals(href)) {
                status = checkLinkInternal(decodedLink, resourceResolver);
            }
        }
        return status;
    }

    private Status checkLinkInternal(String href, ResourceResolver resourceResolver) {
        return Optional.of(resourceResolver.resolve(href))
                .filter(resource -> !ResourceUtil.isNonExistingResource(resource))
                .map(resource -> new Status(HttpStatus.SC_OK, "OK"))
                .orElse(new Status(HttpStatus.SC_NOT_FOUND, "Not Found"));
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