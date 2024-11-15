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

package com.exadel.etoolbox.linkinspector.core.services.resolvers.configs;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "EToolbox Link Inspector - Link Helper",
        description = "Assists in link processing"
)
public @interface InternalLinkResolverConfig {

    @AttributeDefinition(
            name = "Internal Links Host",
            description = "Host to be used for verifying internal links. " +
                    "If no value is set, links will be verified against local JCR.")
    String internalLinksHost() default StringUtils.EMPTY;

    @AttributeDefinition(
            name = "Enable service",
            description = "Is service active or not"
    ) boolean linkType() default false;
}
