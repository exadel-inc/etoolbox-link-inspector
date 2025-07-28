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
import com.exadel.etoolbox.linkinspector.core.services.resolvers.configs.TextResolverConfig;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p><u>Note</u>: This class is not a part of the public API and is subject to change. Do not use it in your own code</p>
 * Validates external links via sending HEAD requests concurrently using {@link PoolingHttpClientConnectionManager}
 */
@Component(service = Resolver.class, immediate = true)
@Designate(ocd = TextResolverConfig.class)
@Slf4j
public class TextResolverImpl implements Resolver {

    private static final String TYPE_TEXT = "Text";
    private static final Status STATUS_FOUND = new Status(HttpStatus.SC_OK, "Found");

    private boolean enabled;
    private Pattern search;

    @Activate
    @Modified
    private void activate(TextResolverConfig config) {
        this.enabled = config.enabled();
        try {
            this.search = Pattern.compile(config.search(), config.caseSensitive() ? 0 : Pattern.CASE_INSENSITIVE);
        } catch (IllegalArgumentException | NullPointerException e) {
            log.error("Invalid search pattern: {}", config.search(), e);
            this.enabled = false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return TYPE_TEXT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Result> getResults(String source) {
        if (!enabled) {
            return Collections.emptyList();
        }

        Matcher matcher = search.matcher(source);
        Set<Result> result = new LinkedHashSet<>();
        while (matcher.find()) {
            result.add(new TextResult(source, matcher.group()));
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate(Result result, ResourceResolver resourceResolver) {
        // No operation
    }

    /**
     * Internal implementation of {@link Result} specific to text pattern matches.
     * <p>
     * This class represents a match of the configured text pattern in content.
     * It always reports a "Found" status and is considered reportable.
     */
    @RequiredArgsConstructor
    @Getter
    @EqualsAndHashCode
    private static class TextResult implements Result {
        private final String value;
        private final String match;

        /**
         * {@inheritDoc}
         */
        @Override
        public String getType() {
            return TYPE_TEXT;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Status getStatus() {
            return STATUS_FOUND;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isReported() {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setStatus(Status status) {
            // No operation
        }
    }
}