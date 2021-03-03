package com.exadel.aembox.linkchecker.core.services.impl;

import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import junitx.util.PrivateAccessor;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(AemContextExtension.class)
class ExternalLinkCheckerImplTest {
    private static final String CLIENT_BUILDER_FACTORY_FIELD = "httpClientBuilderFactory";

    private static final int DEFAULT_CONNECTION_TIMEOUT = 5000;
    private static final int DEFAULT_SOCKET_TIMEOUT = 15000;

    private static final String TEST_EXTERNAL_LINK = "https://www.google.com/test.html";
    private static final String TEST_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36";

    private final ExternalLinkCheckerImpl fixture = new ExternalLinkCheckerImpl();

    private CloseableHttpClient httpClient;
    private CloseableHttpResponse httpResp;

    @BeforeEach
    void setup() throws NoSuchFieldException {
        httpClient = mock(CloseableHttpClient.class);
        httpResp = mock(CloseableHttpResponse.class);

        HttpClientBuilderFactory httpClientBuilderFactory = mock(HttpClientBuilderFactory.class);
        PrivateAccessor.setField(fixture, CLIENT_BUILDER_FACTORY_FIELD, httpClientBuilderFactory);

        HttpClientBuilder clientBuilder = mock(HttpClientBuilder.class, Mockito.CALLS_REAL_METHODS);

        when(httpClientBuilderFactory.newBuilder()).thenReturn(clientBuilder);
        when(clientBuilder.build()).thenReturn(httpClient);

        setUpConfig();
    }

    @Test
    void testCheckLink_nullResponse() throws IOException, URISyntaxException {
        int statusCode = fixture.checkLink(TEST_EXTERNAL_LINK);
        assertEquals(HttpStatus.SC_BAD_REQUEST, statusCode);
    }

    @Test
    void testCheckLink_returnStatusCode() throws IOException, URISyntaxException {
        StatusLine statusLine = mock(StatusLine.class);
        when(httpClient.execute(any(HttpHead.class))).thenReturn(httpResp);
        when(httpResp.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_NOT_FOUND);

        assertEquals(HttpStatus.SC_NOT_FOUND, fixture.checkLink(TEST_EXTERNAL_LINK));
    }

    @Test
    void testDeactivate() throws IOException {
        fixture.deactivate();
        verify(httpClient).close();
    }

    private void setUpConfig() {
        ExternalLinkCheckerImpl.Configuration config = mock(ExternalLinkCheckerImpl.Configuration.class);
        when(config.connection_timeout()).thenReturn(DEFAULT_CONNECTION_TIMEOUT);
        when(config.socket_timeout()).thenReturn(DEFAULT_SOCKET_TIMEOUT);
        when(config.user_agent()).thenReturn(TEST_USER_AGENT);

        fixture.activate(config);
    }
}