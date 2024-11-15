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
        name = "EToolbox Link Inspector - External Link Resolver",
        description = "Validates external links"
)
public @interface ExternalLinkResolverConfig {
    int DEFAULT_CONNECTION_TIMEOUT = 5000;
    int DEFAULT_SOCKET_TIMEOUT = 15000;

    @AttributeDefinition(
            name = "Connection timeout",
            description = "The time (in milliseconds) for connection to disconnect"
    ) int connectionTimeout() default DEFAULT_CONNECTION_TIMEOUT;

    @AttributeDefinition(
            name = "Socket timeout",
            description = "The timeout (in milliseconds) for socket"
    ) int socketTimeout() default DEFAULT_SOCKET_TIMEOUT;

    @AttributeDefinition(
            name = "User agent",
            description = "Example - Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like " +
                    "Gecko) Chrome/86.0.4240.111 Safari/537.36"
    ) String userAgent() default StringUtils.EMPTY;

    @AttributeDefinition(
            name = "Enable service",
            description = "Is service active or not"
    ) boolean linkType() default false;
}
