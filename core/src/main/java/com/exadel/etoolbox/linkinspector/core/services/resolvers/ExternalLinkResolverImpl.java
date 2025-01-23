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
import com.exadel.etoolbox.linkinspector.api.LinkResolver;
import com.exadel.etoolbox.linkinspector.core.models.LinkResult;
import com.exadel.etoolbox.linkinspector.core.services.resolvers.configs.ExternalLinkResolverConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.http.util.EntityUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
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
@Component(service = {LinkResolver.class, ExternalLinkResolverImpl.class}, immediate = true)
@Designate(ocd = ExternalLinkResolverConfig.class)
public class ExternalLinkResolverImpl implements LinkResolver {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalLinkResolverImpl.class);

    private static final int DEFAULT_CONNECTION_TIMEOUT = 5000;
    private static final int DEFAULT_MAX_TOTAL = 1000;
    private static final int DEFAULT_MAX_PER_ROUTE = 1000;

    private static final Pattern PATTERN_EXTERNAL_LINK = Pattern.compile("https?://[\\w\\d-]+\\.[^\\s\"'<]{2," +
            "}|www\\d*\\.[\\w\\d-]+\\.[^\\s\"'<]{2,}");

    @Reference
    private HttpClientBuilderFactory httpClientBuilderFactory;

    private CloseableHttpClient httpClient;
    private PoolingHttpClientConnectionManager connectionManager;

    private int connectionTimeout;
    private int socketTimeout;
    private String userAgent;
    private boolean enabled;

    @Override
    public String getId() {
        return "External";
    }

    @Override
    public Collection<Result> getLinks(String source) {
        if (!enabled) {
            return Collections.emptyList();
        }
        Set<Result> results = new HashSet<>();
        Matcher matcher = PATTERN_EXTERNAL_LINK.matcher(source);
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
        try {
            int statusCode = checkLink(result.getValue());
            result.setStatus(statusCode);
        } catch (SocketTimeoutException e) {
            LOG.error("Timeout occurred while validating link {}", result.getValue(), e);
            result.setStatus(HttpStatus.SC_REQUEST_TIMEOUT, "Request Timeout");
        } catch (UnknownHostException e) {
            LOG.error("Unknown host detected when validating {}", result.getValue(), e);
            result.setStatus(HttpStatus.SC_NOT_FOUND, "Unknown host");
        } catch (URISyntaxException e) {
            LOG.error("Invalid URI syntax when validating link {}", result.getValue(), e);
            result.setStatus(HttpStatus.SC_BAD_REQUEST, "Invalid URI syntax");
        } catch (Exception e) {
            LOG.error("Failed to validate link {}", result.getValue(), e);
            result.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR, StringUtils.defaultIfEmpty(e.getMessage(), e.toString()));
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Activate
    @Modified
    void activate(ExternalLinkResolverConfig config) {
        enabled = config.enabled();
        connectionTimeout = config.connectionTimeout();
        socketTimeout = config.socketTimeout();
        userAgent = config.userAgent();
        buildCloseableHttpClient();
    }

    @Deactivate
    void deactivate() {
        if (this.httpClient != null) {
            try {
                this.httpClient.close();
            } catch (IOException e) {
                LOG.error("Failed to close httpClient", e);
            }
        }
        Optional.ofNullable(connectionManager)
                .ifPresent(PoolingHttpClientConnectionManager::close);
    }

    private int checkLink(String url) throws URISyntaxException, IOException {
        URI uri = new URI(url);
        int statusCode = checkLink(url, new HttpHead(uri));
        if (statusCode != HttpStatus.SC_OK) {
            statusCode = checkLink(url, new HttpGet(uri));
        }
        return statusCode;
    }

    private int checkLink(String url, HttpRequestBase method) throws IOException {
        try (CloseableHttpResponse httpResp = this.httpClient.execute(method)) {
            if (httpResp == null) {
                LOG.error("Failed to get response from server while performing request, url: {}", url);
                return HttpStatus.SC_BAD_REQUEST;
            }
            int statusCode = httpResp.getStatusLine().getStatusCode();
            EntityUtils.consumeQuietly(httpResp.getEntity());
            LOG.trace("PoolingHttpClientConnectionManager leased: {}, link: {}",
                    connectionManager.getTotalStats().getLeased(), url);
            return statusCode;
        } finally {
            Optional.ofNullable(method).ifPresent(HttpRequestBase::releaseConnection);
        }
    }

    private void buildCloseableHttpClient() {
        connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(DEFAULT_MAX_TOTAL);
        connectionManager.setDefaultMaxPerRoute(DEFAULT_MAX_PER_ROUTE);
        HttpClientBuilder clientBuilder = this.httpClientBuilderFactory.newBuilder();
        if (clientBuilder == null) {
            return;
        }
        clientBuilder.setConnectionManager(connectionManager);
        Optional.ofNullable(userAgent)
                .filter(StringUtils::isNotBlank)
                .ifPresent(clientBuilder::setUserAgent);
        if (connectionTimeout >= DEFAULT_CONNECTION_TIMEOUT) {
            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(connectionTimeout)
                    .setConnectionRequestTimeout(socketTimeout)
                    .setSocketTimeout(socketTimeout)
                    .build();
            this.httpClient = clientBuilder
                    .setDefaultRequestConfig(config)
                    .build();
        } else {
            this.httpClient = clientBuilder.build();
        }
    }
}