package com.exadel.etoolbox.linkinspector.core.services.resolvers.configs;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "EToolbox Link Inspector - Text Resolver",
        description = "Searches for particular text in content"
)
public @interface TextResolverConfig {
    @AttributeDefinition(
            name = "Enabled",
            description = "Is service enabled?"
    )
    boolean enabled() default true;

    @AttributeDefinition(
            name = "Text to look for",
            description = "Enter a string to look for (can be a RegExp)"
    )
    String search();

    @AttributeDefinition(
            name = "Case sensitive",
            description = "Whether the search is case-sensitive"
    )
    boolean caseSensitive() default false;
}

