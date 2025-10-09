package com.exadel.etoolbox.linkinspector.core.services.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.annotation.versioning.ProviderType;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@ProviderType
@Slf4j
public class SlingUriBuilder {
    private static final String HTTPS_SCHEME = "https";
    private static final int HTTPS_DEFAULT_PORT = 443;
    private static final String HTTP_SCHEME = "http";
    private static final int HTTP_DEFAULT_PORT = 80;
    private static final String FILE_SCHEME = "file";
    static final String CHAR_HASH = "#";
    static final String CHAR_QM = "?";
    static final char CHAR_AMP = '&';
    static final char CHAR_AT = '@';
    static final char CHAR_SEMICOLON = ';';
    static final char CHAR_EQUALS = '=';
    static final char CHAR_SINGLEQUOTE = '\'';
    static final String CHAR_COLON = ":";
    static final String CHAR_DOT = ".";
    static final String CHAR_SLASH = "/";
    static final String SELECTOR_DOT_REGEX = "\\.(?!\\.?/)";
    static final String PATH_PARAMETERS_REGEX = ";([a-zA-z0-9]+)=(?:\\'([^']*)\\'|([^/]+))";
    static final String BEST_EFFORT_INVALID_URI_MATCHER = "^(?:([^:#@]+):)?(?://(?:([^@#]+)@)?([^/#:]+)(?::([0-9]+))?)?(?:([^?#]+))?(?:\\?([^#]*))?(?:#(.*))?$";
    private String scheme = null;
    private String userInfo = null;
    private String host = null;
    private int port = -1;
    private String resourcePath = null;
    private final List<String> selectors = new LinkedList();
    private String extension = null;
    private final Map<String, String> pathParameters = new LinkedHashMap();
    private String suffix = null;
    private String schemeSpecificPart = null;
    private String query = null;
    private String fragment = null;
    private ResourceResolver resourceResolver = null;
    private boolean isBuilt = false;

    public static SlingUriBuilder create() {
        return new SlingUriBuilder();
    }

    public static SlingUriBuilder createFrom(SlingUri slingUri) {
        return create().setScheme(slingUri.getScheme()).setUserInfo(slingUri.getUserInfo()).setHost(slingUri.getHost()).setPort(slingUri.getPort()).setResourcePath(slingUri.getResourcePath()).setPathParameters(slingUri.getPathParameters()).setSelectors(slingUri.getSelectors()).setExtension(slingUri.getExtension()).setSuffix(slingUri.getSuffix()).setQuery(slingUri.getQuery()).setFragment(slingUri.getFragment()).setSchemeSpecificPart(slingUri.isOpaque() ? slingUri.getSchemeSpecificPart() : null).setResourceResolver(slingUri instanceof SlingUriBuilder.ImmutableSlingUri ? ((SlingUriBuilder.ImmutableSlingUri)slingUri).getData().resourceResolver : null);
    }

    public static SlingUriBuilder createFrom(Resource resource) {
        return create().setResourcePath(resource.getPath()).setResourceResolver(resource.getResourceResolver());
    }

    public static SlingUriBuilder createFrom(RequestPathInfo requestPathInfo) {
        Resource suffixResource = requestPathInfo.getSuffixResource();
        return create().setResourceResolver(suffixResource != null ? suffixResource.getResourceResolver() : null).setResourcePath(requestPathInfo.getResourcePath()).setSelectors(requestPathInfo.getSelectors()).setExtension(requestPathInfo.getExtension()).setSuffix(requestPathInfo.getSuffix());
    }

    public static SlingUriBuilder createFrom(SlingHttpServletRequest request) {
        ResourceResolver resourceResolver = request.getResourceResolver();
        SlingUriBuilder uriBuilder = createFrom(request.getRequestPathInfo()).setResourceResolver(resourceResolver).setScheme(request.getScheme()).setHost(request.getServerName()).setPort(request.getServerPort()).setQuery(request.getQueryString());
        String resourcePath = uriBuilder.getResourcePath();
        if (resourcePath != null) {
            String mappedResourcePath = resourceResolver.map(request, resourcePath);
            if (!resourcePath.equals(mappedResourcePath) && request.getPathInfo().startsWith(mappedResourcePath)) {
                uriBuilder.setResourcePath(mappedResourcePath);
            }
        }

        return uriBuilder;
    }

    public static SlingUriBuilder createFrom(URI uri, ResourceResolver resourceResolver) {
        String path = uri.getRawPath();
        boolean pathExists = isNotBlank(path);
        String uriQuery = uri.getRawQuery();
        boolean schemeSpecificRelevant = !pathExists && uriQuery == null;
        String uriHost = uri.getHost();
        if ("file".equals(uri.getScheme()) && uriHost == null) {
            uriHost = "";
        }

        return create().setResourceResolver(resourceResolver).setScheme(uri.getScheme()).setUserInfo(uri.getRawUserInfo()).setHost(uriHost).setPort(uri.getPort()).setPath(pathExists ? path : null).setQuery(uriQuery).setFragment(uri.getRawFragment()).setSchemeSpecificPart(schemeSpecificRelevant ? uri.getRawSchemeSpecificPart() : null);
    }

    public static SlingUriBuilder parse(String uriStr, ResourceResolver resourceResolver) {
        try {
            URI uri = new URI(uriStr);
            return createFrom(uri, resourceResolver);
        } catch (URISyntaxException e) {
            log.debug("Invalid URI {}: {}", new Object[]{uriStr, e.getMessage(), e});
            return parseBestEffort(uriStr, resourceResolver);
        }
    }

    private static SlingUriBuilder parseBestEffort(String uriStr, ResourceResolver resourceResolver) {
        Matcher matcher = Pattern.compile("^(?:([^:#@]+):)?(?://(?:([^@#]+)@)?([^/#:]+)(?::([0-9]+))?)?(?:([^?#]+))?(?:\\?([^#]*))?(?:#(.*))?$").matcher(uriStr);
        matcher.find();
        String scheme = matcher.group(1);
        String userInfo = matcher.group(2);
        String host = matcher.group(3);
        String port = matcher.groupCount() >= 4 ? matcher.group(4) : null;
        String path = matcher.groupCount() >= 5 ? matcher.group(5) : null;
        String query = matcher.groupCount() >= 6 ? matcher.group(6) : null;
        String fragment = matcher.groupCount() >= 7 ? matcher.group(7) : null;
        if (!isBlank(scheme) && isBlank(host)) {
            return create().setResourceResolver(resourceResolver).setScheme(scheme).setSchemeSpecificPart(path).setFragment(fragment);
        } else {
            return isBlank(host) && isBlank(path) ? create().setResourceResolver(resourceResolver).setSchemeSpecificPart(uriStr) : create().setResourceResolver(resourceResolver).setScheme(scheme).setUserInfo(userInfo).setHost(host).setPort(port != null ? Integer.parseInt(port) : -1).setPath(path).setQuery(query).setFragment(fragment);
        }
    }

    private static boolean isBlank(CharSequence cs) {
        return cs == null || cs.chars().allMatch(Character::isWhitespace);
    }

    private static boolean isNotBlank(CharSequence cs) {
        return !isBlank(cs);
    }

    private SlingUriBuilder() {
    }

    public SlingUriBuilder setUserInfo(String userInfo) {
        if (this.schemeSpecificPart != null) {
            return this;
        } else {
            this.userInfo = userInfo;
            return this;
        }
    }

    public SlingUriBuilder setHost(String host) {
        if (this.schemeSpecificPart != null) {
            return this;
        } else {
            this.host = host;
            return this;
        }
    }

    public SlingUriBuilder setPort(int port) {
        if (this.schemeSpecificPart != null) {
            return this;
        } else {
            this.port = port;
            return this;
        }
    }

    public SlingUriBuilder setPath(String path) {
        if (this.schemeSpecificPart != null) {
            return this;
        } else {
            path = this.extractPathParameters(path);
            if (path != null && path.startsWith("/") && this.resourceResolver != null) {
                this.setResourcePath(path);
                this.rebaseResourcePath();
            } else {
                Matcher dotMatcher;
                if (path != null && (dotMatcher = Pattern.compile("\\.(?!\\.?/)").matcher(path)).find()) {
                    int firstDotPosition = dotMatcher.start();
                    this.setPathWithDefinedResourcePosition(path, firstDotPosition);
                } else {
                    this.setSelectors(new String[0]);
                    this.setSuffix((String)null);
                    this.setExtension((String)null);
                    this.setResourcePath(path);
                }
            }

            return this;
        }
    }

    public SlingUriBuilder rebaseResourcePath() {
        if (this.schemeSpecificPart == null && this.resourcePath != null) {
            if (this.resourceResolver == null) {
                throw new IllegalStateException("setResourceResolver() needs to be called before balanceResourcePath()");
            } else {
                String path = this.assemblePath(false);
                if (path == null) {
                    return this;
                } else {
                    SlingUriBuilder.ResourcePathIterator it = new SlingUriBuilder.ResourcePathIterator(path);
                    String availableResourcePath = null;

                    while(it.hasNext()) {
                        availableResourcePath = it.next();
                        if (this.resourceResolver.getResource(availableResourcePath) != null) {
                            break;
                        }
                    }

                    if (availableResourcePath == null) {
                        return this;
                    } else {
                        this.selectors.clear();
                        this.extension = null;
                        this.suffix = null;
                        if (availableResourcePath.length() == path.length()) {
                            this.resourcePath = availableResourcePath;
                        } else {
                            this.setPathWithDefinedResourcePosition(path, availableResourcePath.length());
                        }

                        return this;
                    }
                }
            }
        } else {
            return this;
        }
    }

    public SlingUriBuilder setResourcePath(String resourcePath) {
        if (this.schemeSpecificPart != null) {
            return this;
        } else {
            this.resourcePath = resourcePath;
            return this;
        }
    }

    public SlingUriBuilder setSelectors(String[] selectors) {
        if (this.schemeSpecificPart == null && this.resourcePath != null) {
            this.selectors.clear();
            if (selectors != null) {
                Stream var10000 = Arrays.stream(selectors);
                List var10001 = this.selectors;
                Objects.requireNonNull(var10001);
                var10000.forEach(var10001::add);
            }

            return this;
        } else {
            return this;
        }
    }

    public SlingUriBuilder addSelector(String selector) {
        if (this.schemeSpecificPart == null && this.resourcePath != null) {
            this.selectors.add(selector);
            return this;
        } else {
            return this;
        }
    }

    public SlingUriBuilder removeSelector(String selector) {
        if (this.schemeSpecificPart == null && this.resourcePath != null) {
            this.selectors.remove(selector);
            return this;
        } else {
            return this;
        }
    }

    public SlingUriBuilder setExtension(String extension) {
        if (this.schemeSpecificPart == null && this.resourcePath != null) {
            this.extension = extension;
            return this;
        } else {
            return this;
        }
    }

    public SlingUriBuilder setPathParameter(String key, String value) {
        if (this.schemeSpecificPart == null && this.resourcePath != null) {
            this.pathParameters.put(key, value);
            return this;
        } else {
            return this;
        }
    }

    public SlingUriBuilder setPathParameters(Map<String, String> pathParameters) {
        this.pathParameters.clear();
        this.pathParameters.putAll(pathParameters);
        return this;
    }

    public SlingUriBuilder setSuffix(String suffix) {
        if (this.schemeSpecificPart == null && this.resourcePath != null) {
            if (suffix != null && !suffix.startsWith("/")) {
                throw new IllegalArgumentException("Suffix needs to start with slash");
            } else {
                this.suffix = suffix;
                return this;
            }
        } else {
            return this;
        }
    }

    public SlingUriBuilder setQuery(String query) {
        if (this.schemeSpecificPart != null) {
            return this;
        } else {
            this.query = query;
            return this;
        }
    }

    public SlingUriBuilder addQueryParameter(String parameterName, String value) {
        if (this.schemeSpecificPart != null) {
            return this;
        } else {
            try {
                this.query = (this.query == null ? "" : this.query + '&') + URLEncoder.encode(parameterName, StandardCharsets.UTF_8.name()) + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8.name());
                return this;
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException("Encoding not supported: " + StandardCharsets.UTF_8, e);
            }
        }
    }

    public SlingUriBuilder setQueryParameters(Map<String, String> queryParameters) {
        if (this.schemeSpecificPart != null) {
            return this;
        } else {
            this.setQuery((String)null);

            for(Map.Entry<String, String> parameter : queryParameters.entrySet()) {
                this.addQueryParameter((String)parameter.getKey(), (String)parameter.getValue());
            }

            return this;
        }
    }

    public SlingUriBuilder setFragment(String fragment) {
        this.fragment = fragment;
        return this;
    }

    public SlingUriBuilder setScheme(String scheme) {
        this.scheme = scheme;
        return this;
    }

    public SlingUriBuilder setSchemeSpecificPart(String schemeSpecificPart) {
        this.schemeSpecificPart = schemeSpecificPart;
        return this;
    }

    public SlingUriBuilder removeSchemeAndAuthority() {
        this.setScheme((String)null);
        this.setUserInfo((String)null);
        this.setHost((String)null);
        this.setPort(-1);
        return this;
    }

    public SlingUriBuilder useSchemeAndAuthority(SlingUri slingUri) {
        this.setScheme(slingUri.getScheme());
        this.setUserInfo(slingUri.getUserInfo());
        this.setHost(slingUri.getHost());
        this.setPort(slingUri.getPort());
        return this;
    }

    public String getResourcePath() {
        return this.resourcePath;
    }

    public String getSelectorString() {
        return !this.selectors.isEmpty() ? String.join(".", this.selectors) : null;
    }

    public String[] getSelectors() {
        return (String[])this.selectors.toArray(new String[this.selectors.size()]);
    }

    public String getExtension() {
        return this.extension;
    }

    public Map<String, String> getPathParameters() {
        return this.pathParameters;
    }

    public String getSuffix() {
        return this.suffix;
    }

    public Resource getSuffixResource() {
        return isNotBlank(this.suffix) && this.resourceResolver != null ? this.resourceResolver.getResource(this.suffix) : null;
    }

    public String getPath() {
        return this.assemblePath(true);
    }

    public String getSchemeSpecificPart() {
        return this.isOpaque() ? this.schemeSpecificPart : this.toStringInternal(false, false);
    }

    public String getQuery() {
        return this.query;
    }

    public String getFragment() {
        return this.fragment;
    }

    public String getScheme() {
        return this.scheme;
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    public String getUserInfo() {
        return this.userInfo;
    }

    public SlingUriBuilder useSchemeAndAuthority(URI uri) {
        this.useSchemeAndAuthority(createFrom(uri, this.resourceResolver).build());
        return this;
    }

    public SlingUriBuilder setResourceResolver(ResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
        return this;
    }

    public SlingUri build() {
        if (this.isBuilt) {
            throw new IllegalStateException("SlingUriBuilder.build() may only be called once per builder instance");
        } else {
            this.isBuilt = true;
            return new SlingUriBuilder.ImmutableSlingUri();
        }
    }

    public String toString() {
        return this.toStringInternal(true, true);
    }

    public boolean isPath() {
        return isBlank(this.scheme) && isBlank(this.host) && isNotBlank(this.resourcePath);
    }

    public boolean isAbsolutePath() {
        return this.isPath() && this.resourcePath.startsWith("/");
    }

    public boolean isRelativePath() {
        return this.isPath() && !this.resourcePath.startsWith("/");
    }

    public boolean isAbsolute() {
        return this.scheme != null;
    }

    public boolean isOpaque() {
        return this.scheme != null && this.schemeSpecificPart != null;
    }

    private String toStringInternal(boolean includeScheme, boolean includeFragment) {
        StringBuilder requestUri = new StringBuilder();
        if (includeScheme && this.isAbsolute()) {
            requestUri.append(this.scheme + ":");
        }

        if (this.host != null) {
            requestUri.append("//");
            if (isNotBlank(this.userInfo)) {
                requestUri.append(this.userInfo + '@');
            }

            requestUri.append(this.host);
            if (this.port > 0 && (!"http".equals(this.scheme) || this.port != 80) && (!"https".equals(this.scheme) || this.port != 443)) {
                requestUri.append(":");
                requestUri.append(this.port);
            }
        }

        if (this.schemeSpecificPart != null) {
            requestUri.append(this.schemeSpecificPart);
        }

        if (this.resourcePath != null) {
            requestUri.append(this.assemblePath(true));
        }

        if (this.query != null) {
            requestUri.append("?" + this.query);
        }

        if (includeFragment && this.fragment != null) {
            requestUri.append("#" + this.fragment);
        }

        return requestUri.toString();
    }

    private void setPathWithDefinedResourcePosition(String path, int firstDotPositionAfterResourcePath) {
        this.setResourcePath(path.substring(0, firstDotPositionAfterResourcePath));
        int firstSlashAfterFirstDotPosition = path.indexOf("/", firstDotPositionAfterResourcePath);
        String pathWithoutSuffix = firstSlashAfterFirstDotPosition > -1 ? path.substring(firstDotPositionAfterResourcePath + 1, firstSlashAfterFirstDotPosition) : path.substring(firstDotPositionAfterResourcePath + 1);
        String[] pathBits = pathWithoutSuffix.split("\\.(?!\\.?/)");
        if (pathBits.length > 1) {
            this.setSelectors((String[])Arrays.copyOfRange(pathBits, 0, pathBits.length - 1));
        }

        this.setExtension(pathBits.length > 0 && pathBits[pathBits.length - 1].length() > 0 ? pathBits[pathBits.length - 1] : null);
        this.setSuffix(firstSlashAfterFirstDotPosition > -1 ? path.substring(firstSlashAfterFirstDotPosition) : null);
    }

    private String extractPathParameters(String path) {
        this.pathParameters.clear();
        if (path != null) {
            Pattern pathParameterRegex = Pattern.compile(";([a-zA-z0-9]+)=(?:\\'([^']*)\\'|([^/]+))");
            StringBuffer resultString = null;
            Matcher regexMatcher = pathParameterRegex.matcher(path);

            while(regexMatcher.find()) {
                if (resultString == null) {
                    resultString = new StringBuffer();
                }

                regexMatcher.appendReplacement(resultString, "");
                String key = regexMatcher.group(1);
                String value = isNotBlank(regexMatcher.group(2)) ? regexMatcher.group(2) : regexMatcher.group(3);
                this.pathParameters.put(key, value);
            }

            if (resultString != null) {
                regexMatcher.appendTail(resultString);
                path = resultString.toString();
            }
        }

        return path;
    }

    private String assemblePath(boolean includePathParamters) {
        if (this.resourcePath == null) {
            return null;
        } else {
            StringBuilder pathBuilder = new StringBuilder();
            pathBuilder.append(this.resourcePath);
            if (includePathParamters && !this.pathParameters.isEmpty()) {
                for(Map.Entry<String, String> pathParameter : this.pathParameters.entrySet()) {
                    pathBuilder.append(';' + (String)pathParameter.getKey() + '=' + '\'' + (String)pathParameter.getValue() + '\'');
                }
            }

            boolean dotAdded = false;
            if (!this.selectors.isEmpty()) {
                pathBuilder.append("." + String.join(".", this.selectors));
                dotAdded = true;
            }

            if (isNotBlank(this.extension)) {
                pathBuilder.append("." + this.extension);
                dotAdded = true;
            }

            if (isNotBlank(this.suffix)) {
                if (!dotAdded) {
                    pathBuilder.append(".");
                }

                pathBuilder.append(this.suffix);
            }

            return pathBuilder.toString();
        }
    }

    private class ImmutableSlingUri implements SlingUri {
        private ImmutableSlingUri() {
        }

        public String getResourcePath() {
            return this.getData().getResourcePath();
        }

        public String getSelectorString() {
            return this.getData().getSelectorString();
        }

        public String[] getSelectors() {
            return this.getData().getSelectors();
        }

        public String getExtension() {
            return this.getData().getExtension();
        }

        public Map<String, String> getPathParameters() {
            return Collections.unmodifiableMap(this.getData().getPathParameters());
        }

        public String getSuffix() {
            return this.getData().getSuffix();
        }

        public String getPath() {
            return this.getData().getPath();
        }

        public String getSchemeSpecificPart() {
            return this.getData().getSchemeSpecificPart();
        }

        public String getQuery() {
            return this.getData().getQuery();
        }

        public String getFragment() {
            return this.getData().getFragment();
        }

        public String getScheme() {
            return this.getData().getScheme();
        }

        public String getHost() {
            return this.getData().getHost();
        }

        public int getPort() {
            return this.getData().getPort();
        }

        public Resource getSuffixResource() {
            return this.getData().getSuffixResource();
        }

        public String getUserInfo() {
            return this.getData().getUserInfo();
        }

        public boolean isOpaque() {
            return this.getData().isOpaque();
        }

        public boolean isPath() {
            return this.getData().isPath();
        }

        public boolean isAbsolutePath() {
            return this.getData().isAbsolutePath();
        }

        public boolean isRelativePath() {
            return this.getData().isRelativePath();
        }

        public boolean isAbsolute() {
            return this.getData().isAbsolute();
        }

        public String toString() {
            return this.getData().toString();
        }

        public URI toUri() {
            String uriString = this.toString();

            try {
                return new URI(uriString);
            } catch (URISyntaxException e) {
                throw new IllegalStateException("Invalid Sling URI: " + uriString, e);
            }
        }

        private SlingUriBuilder getData() {
            return SlingUriBuilder.this;
        }

        public int hashCode() {
            int prime = 31;
            int result = 1;
            result = 31 * result + (SlingUriBuilder.this.extension == null ? 0 : SlingUriBuilder.this.extension.hashCode());
            result = 31 * result + (SlingUriBuilder.this.fragment == null ? 0 : SlingUriBuilder.this.fragment.hashCode());
            result = 31 * result + (SlingUriBuilder.this.host == null ? 0 : SlingUriBuilder.this.host.hashCode());
            result = 31 * result + SlingUriBuilder.this.pathParameters.hashCode();
            result = 31 * result + SlingUriBuilder.this.port;
            result = 31 * result + (SlingUriBuilder.this.query == null ? 0 : SlingUriBuilder.this.query.hashCode());
            result = 31 * result + (SlingUriBuilder.this.resourcePath == null ? 0 : SlingUriBuilder.this.resourcePath.hashCode());
            result = 31 * result + (SlingUriBuilder.this.scheme == null ? 0 : SlingUriBuilder.this.scheme.hashCode());
            result = 31 * result + SlingUriBuilder.this.schemeSpecificPart == null ? 0 : SlingUriBuilder.this.schemeSpecificPart.hashCode();
            result = 31 * result + SlingUriBuilder.this.selectors.hashCode();
            result = 31 * result + (SlingUriBuilder.this.suffix == null ? 0 : SlingUriBuilder.this.suffix.hashCode());
            result = 31 * result + (SlingUriBuilder.this.userInfo == null ? 0 : SlingUriBuilder.this.userInfo.hashCode());
            return result;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj == null) {
                return false;
            } else if (this.getClass() != obj.getClass()) {
                return false;
            } else {
                SlingUriBuilder.ImmutableSlingUri other = (SlingUriBuilder.ImmutableSlingUri)obj;
                if (SlingUriBuilder.this.extension == null) {
                    if (other.getData().extension != null) {
                        return false;
                    }
                } else if (!SlingUriBuilder.this.extension.equals(other.getData().extension)) {
                    return false;
                }

                if (SlingUriBuilder.this.fragment == null) {
                    if (other.getData().fragment != null) {
                        return false;
                    }
                } else if (!SlingUriBuilder.this.fragment.equals(other.getData().fragment)) {
                    return false;
                }

                if (SlingUriBuilder.this.host == null) {
                    if (other.getData().host != null) {
                        return false;
                    }
                } else if (!SlingUriBuilder.this.host.equals(other.getData().host)) {
                    return false;
                }

                if (SlingUriBuilder.this.pathParameters == null) {
                    if (other.getData().pathParameters != null) {
                        return false;
                    }
                } else if (!SlingUriBuilder.this.pathParameters.equals(other.getData().pathParameters)) {
                    return false;
                }

                if (SlingUriBuilder.this.port != other.getData().port) {
                    return false;
                } else {
                    if (SlingUriBuilder.this.query == null) {
                        if (other.getData().query != null) {
                            return false;
                        }
                    } else if (!SlingUriBuilder.this.query.equals(other.getData().query)) {
                        return false;
                    }

                    if (SlingUriBuilder.this.resourcePath == null) {
                        if (other.getData().resourcePath != null) {
                            return false;
                        }
                    } else if (!SlingUriBuilder.this.resourcePath.equals(other.getData().resourcePath)) {
                        return false;
                    }

                    if (SlingUriBuilder.this.scheme == null) {
                        if (other.getData().scheme != null) {
                            return false;
                        }
                    } else if (!SlingUriBuilder.this.scheme.equals(other.getData().scheme)) {
                        return false;
                    }

                    if (SlingUriBuilder.this.schemeSpecificPart == null) {
                        if (other.getData().schemeSpecificPart != null) {
                            return false;
                        }
                    } else if (!SlingUriBuilder.this.schemeSpecificPart.equals(other.getData().schemeSpecificPart)) {
                        return false;
                    }

                    if (SlingUriBuilder.this.selectors == null) {
                        if (other.getData().selectors != null) {
                            return false;
                        }
                    } else if (!SlingUriBuilder.this.selectors.equals(other.getData().selectors)) {
                        return false;
                    }

                    if (SlingUriBuilder.this.suffix == null) {
                        if (other.getData().suffix != null) {
                            return false;
                        }
                    } else if (!SlingUriBuilder.this.suffix.equals(other.getData().suffix)) {
                        return false;
                    }

                    if (SlingUriBuilder.this.userInfo == null) {
                        if (other.getData().userInfo != null) {
                            return false;
                        }
                    } else if (!SlingUriBuilder.this.userInfo.equals(other.getData().userInfo)) {
                        return false;
                    }

                    return true;
                }
            }
        }
    }

    private class ResourcePathIterator implements Iterator<String> {
        private String nextPath;

        private ResourcePathIterator(String path) {
            if (path != null && path.length() != 0) {
                int i;
                for(i = path.length() - 1; i >= 0 && path.charAt(i) == '/'; --i) {
                }

                if (i < 0) {
                    this.nextPath = "/";
                } else if (i < path.length() - 1) {
                    this.nextPath = path.substring(0, i + 1);
                } else {
                    this.nextPath = path;
                }
            } else {
                this.nextPath = null;
            }

        }

        public boolean hasNext() {
            return this.nextPath != null;
        }

        public String next() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            } else {
                String result = this.nextPath;
                int lastDot = this.nextPath.lastIndexOf(46);
                this.nextPath = lastDot > 0 ? this.nextPath.substring(0, lastDot) : null;
                return result;
            }
        }

        public void remove() {
            throw new UnsupportedOperationException("remove");
        }
    }
}

