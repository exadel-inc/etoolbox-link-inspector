package com.exadel.etoolbox.linkinspector.core.services.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.annotation.versioning.ProviderType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@ProviderType
@Slf4j
public class SlingUriBuilder {
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

    public static SlingUriBuilder create() {
        return new SlingUriBuilder();
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

    public SlingUriBuilder setExtension(String extension) {
        if (this.schemeSpecificPart == null && this.resourcePath != null) {
            this.extension = extension;
            return this;
        } else {
            return this;
        }
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

    public String getExtension() {
        return this.extension;
    }

    public String getPath() {
        return this.assemblePath(true);
    }

    public String getQuery() {
        return this.query;
    }

    public SlingUriBuilder setResourceResolver(ResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
        return this;
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

