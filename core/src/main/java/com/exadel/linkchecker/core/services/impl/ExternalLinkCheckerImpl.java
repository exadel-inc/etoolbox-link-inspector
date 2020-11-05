package com.exadel.linkchecker.core.services.impl;

import com.exadel.linkchecker.core.services.ExternalLinkChecker;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.http.util.EntityUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

@Component(service = ExternalLinkChecker.class)
@Designate(ocd = ExternalLinkCheckerImpl.Configuration.class)
public class ExternalLinkCheckerImpl implements ExternalLinkChecker {
    @ObjectClassDefinition(
            name = "Exadel Link Checker - External Links Validator",
            description = "Validates external links"
    )
    @interface Configuration {
        @AttributeDefinition(
                name = "Connection timeout",
                description = "The time (in milliseconds) for connection to disconnect"
        ) int connection_timeout() default DEFAULT_CONNECTION_TIMEOUT;

        @AttributeDefinition(
                name = "Socket timeout",
                description = "The timeout (in milliseconds) for socket"
        ) int socket_timeout() default DEFAULT_SOCKET_TIMEOUT;

        @AttributeDefinition(
                name = "User agent",
                description = "Example - Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36"
        ) String user_agent() default StringUtils.EMPTY;
    }

    private static final Logger LOG = LoggerFactory.getLogger(ExternalLinkChecker.class);
    private static final int DEFAULT_CONNECTION_TIMEOUT = 5000;
    private static final int DEFAULT_SOCKET_TIMEOUT = 15000;

    @Reference
    private HttpClientBuilderFactory httpClientBuilderFactory;

    private CloseableHttpClient httpClient;
    private PoolingHttpClientConnectionManager connectionManager;

    private int connectionTimeout;
    private int socketTimeout;
    private String userAgent;

    @Override
    public int checkLink(String url) throws URISyntaxException, IOException {
        HttpHead headMethod = null;
        try {
            URI uri = new URI(url);
            headMethod = new HttpHead(uri.toString());
            try (CloseableHttpResponse httpResp = this.httpClient.execute(headMethod)) {
                if (httpResp == null) {
                    LOG.error("Failed to get response from server while performing HEAD request, url: {}", url);
                    return HttpStatus.SC_BAD_REQUEST;
                }
                int statusCode = httpResp.getStatusLine().getStatusCode();
                EntityUtils.consumeQuietly(httpResp.getEntity());
                LOG.trace("PoolingHttpClientConnectionManager leased: {}, link: {}",
                        connectionManager.getTotalStats().getLeased(), url);
                return statusCode;
            }
        } finally {
            Optional.ofNullable(headMethod)
                    .ifPresent(HttpRequestBase::releaseConnection);
        }
    }

    @Activate
    @Modified
    protected void activate(Configuration configuration) {
        connectionTimeout = configuration.connection_timeout();
        socketTimeout = configuration.socket_timeout();
        userAgent = configuration.user_agent();
        buildCloseableHttpClient();
    }

    @Deactivate
    protected void deactivate() {
        if (this.httpClient != null) {
            try {
                this.httpClient.close();
            } catch (IOException e) {
                LOG.error("Failed to close httpClient", e);
            }
        }
        Optional.ofNullable(connectionManager).
                ifPresent(PoolingHttpClientConnectionManager::close);
    }

    private void buildCloseableHttpClient() {
        connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(1000);
        connectionManager.setDefaultMaxPerRoute(1000);
        HttpClientBuilder clientBuilder = this.httpClientBuilderFactory.newBuilder()
                .setConnectionManager(connectionManager);
        Optional.of(userAgent)
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