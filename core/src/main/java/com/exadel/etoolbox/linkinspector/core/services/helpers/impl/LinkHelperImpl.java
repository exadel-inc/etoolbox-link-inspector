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

import com.exadel.etoolbox.linkinspector.api.Result;
import com.exadel.etoolbox.linkinspector.api.Resolver;
import com.exadel.etoolbox.linkinspector.api.Status;
import com.exadel.etoolbox.linkinspector.core.services.helpers.LinkHelper;
import com.exadel.etoolbox.linkinspector.core.services.util.LinkInspectorResourceUtil;
import org.apache.http.HttpStatus;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Implements {@link LinkHelper} interface to provide an OSGi service which provides helper methods for processing links
 * <p><u>Note</u>: This class is not a part of the public API and is subject to change. Do not use it in your own code</p>
 */
@Component(
        service = LinkHelper.class
)
public class LinkHelperImpl implements LinkHelper {

    private static final Logger LOG = LoggerFactory.getLogger(LinkHelperImpl.class);

    @Reference(
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY
    )
    private volatile List<Resolver> linkResolvers;

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<Result> getLinkStream(Object source) {
        Stream<Result> linkStream = Stream.empty();
        if (source instanceof String) {
            String stringValue = String.valueOf(source);
            linkStream = getLinkStream(stringValue);
        } else if (source instanceof String[]) {
            linkStream = Arrays.stream((String[]) source)
                    .flatMap(this::getLinkStream);
        }
        return linkStream;
    }

    private Stream<Result> getLinkStream(String source) {
        return linkResolvers
                .stream()
                .flatMap(linkResolver -> linkResolver.getResults(source).stream());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateLink(Result result, ResourceResolver resourceResolver) {
        String type = result.getType();
        Resolver linkResolver = linkResolvers.stream().filter(item -> item.getId().equals(type)).findFirst().orElse(null);
        if (linkResolver == null) {
            result.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Link resolver not found");
            return;
        }
        LOG.trace("Started validation of {}", result.getValue());
        linkResolver.validate(result, resourceResolver);
        LOG.trace("Completed validation of {}", result.getValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Status validateLink(String href, ResourceResolver resourceResolver) {
        Optional<Result> detectedLink = this.getLinkStream(href).findFirst();
        if (!detectedLink.isPresent() || !detectedLink.get().getValue().equals(href)) {
            return new Status(HttpStatus.SC_BAD_REQUEST, "Unsupported link type");
        }
        validateLink(detectedLink.get(), resourceResolver);
        return detectedLink.get().getStatus();
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

    private boolean updateValueMapWithNewLink(ModifiableValueMap modifiableValueMap,
                                              String propertyName, String currentLink, String newLink) {
        boolean updated = false;
        Optional<Object> updatedValue = Optional.ofNullable(modifiableValueMap.get(propertyName))
                .map(value -> LinkInspectorResourceUtil.replaceStringInPropValue(value, currentLink, newLink));
        if (updatedValue.isPresent()) {
            modifiableValueMap.put(propertyName, updatedValue.get());
            updated = true;
        }
        return updated;
    }
}