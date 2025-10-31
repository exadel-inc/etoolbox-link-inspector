package com.exadel.etoolbox.linkinspector.core.services.util;

import java.net.URI;
import java.util.Map;
import java.util.function.Consumer;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface SlingUri extends RequestPathInfo {
    URI toUri();

    String toString();

    String getScheme();

    String getUserInfo();

    String getHost();

    int getPort();

    String getResourcePath();

    String getSelectorString();

    String[] getSelectors();

    String getExtension();

    Map<String, String> getPathParameters();

    String getSuffix();

    String getPath();

    String getQuery();

    String getFragment();

    String getSchemeSpecificPart();

    Resource getSuffixResource();

    boolean isPath();

    boolean isAbsolutePath();

    boolean isRelativePath();

    boolean isAbsolute();

    boolean isOpaque();

    default SlingUri adjust(Consumer<SlingUriBuilder> builderConsumer) {
        SlingUriBuilder builder = SlingUriBuilder.createFrom(this);
        builderConsumer.accept(builder);
        return builder.build();
    }
}
