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
     * Indicates whether this resolver is currently enabled.
     *
     * @return True if the resolver is enabled, false otherwise
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Gets the unique identifier for this resolver.
     *
     * @return String "Text" as the resolver's identifier
     */
    @Override
    public String getId() {
        return TYPE_TEXT;
    }

    /**
     * Finds text patterns in the provided source content based on the configured regular expression.
     * <p>
     * This method searches the source text for matches against the configured pattern
     * and creates a Result object for each match found.
     *
     * @param source The content to search for pattern matches
     * @return Collection of Result objects representing found matches, or empty collection if disabled
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
     * No validation is performed for text matches.
     * <p>
     * This method is a no-operation implementation as text patterns are considered
     * immediately found and don't require additional validation.
     *
     * @param result The Result object containing the text match (not used)
     * @param resourceResolver ResourceResolver (not used)
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
         * Gets the type identifier for this result.
         *
         * @return String "Text" as the result type
         */
        @Override
        public String getType() {
            return TYPE_TEXT;
        }

        /**
         * Gets the status of this text match.
         * <p>
         * Always returns STATUS_FOUND as text matches don't undergo validation.
         *
         * @return Status object with SC_OK and "Found" message
         */
        @Override
        public Status getStatus() {
            return STATUS_FOUND;
        }

        /**
         * Indicates that this result should always be reported.
         *
         * @return True as text matches should always be reported
         */
        @Override
        public boolean isReported() {
            return true;
        }

        /**
         * No-operation implementation as text match status is always "Found".
         *
         * @param status Status to set (ignored)
         */
        @Override
        public void setStatus(Status status) {
            // No operation
        }
    }
}