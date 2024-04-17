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

package com.exadel.etoolbox.linkinspector.core.services.impl;

import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import junitx.util.PrivateAccessor;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
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
import static org.mockito.Mockito.*;

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
    private CloseableHttpResponse httpResp1;

    @BeforeEach
    void setup() throws NoSuchFieldException {
        httpClient = mock(CloseableHttpClient.class);
        httpResp = mock(CloseableHttpResponse.class);
        httpResp1 = mock(CloseableHttpResponse.class);

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
    void testCheckLink_returnStatusCode404whenAll404() throws IOException, URISyntaxException {
        StatusLine statusLine = mock(StatusLine.class);
        when(httpClient.execute(any(HttpHead.class))).thenReturn(httpResp);
        when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResp);
        when(httpResp.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_NOT_FOUND);

        assertEquals(HttpStatus.SC_NOT_FOUND, fixture.checkLink(TEST_EXTERNAL_LINK));
    }

    @Test
    void testCheckLink_returnStatusCode200when404Head200Get() throws IOException, URISyntaxException {
        StatusLine statusLine404 = mock(StatusLine.class);
        when(statusLine404.getStatusCode()).thenReturn(HttpStatus.SC_NOT_FOUND);
        when(httpClient.execute(any(HttpHead.class))).thenReturn(httpResp);
        when(httpResp.getStatusLine()).thenReturn(statusLine404);

        StatusLine statusLine200 = mock(StatusLine.class);
        when(statusLine200.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResp1);
        when(httpResp1.getStatusLine()).thenReturn(statusLine200);

        assertEquals(HttpStatus.SC_OK, fixture.checkLink(TEST_EXTERNAL_LINK));
    }

    @Test
    void testCheckLink_returnStatusCode200when200Head() throws IOException, URISyntaxException {
        StatusLine statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(httpClient.execute(any(HttpHead.class))).thenReturn(httpResp);
        when(httpResp.getStatusLine()).thenReturn(statusLine);

        assertEquals(HttpStatus.SC_OK, fixture.checkLink(TEST_EXTERNAL_LINK));
    }

    @Test
    void testDeactivate() throws IOException {
        fixture.deactivate();
        verify(httpClient).close();
    }

    private void setUpConfig() {
        ExternalLinkCheckerImpl.Configuration config = mock(ExternalLinkCheckerImpl.Configuration.class);
        when(config.connectionTimeout()).thenReturn(DEFAULT_CONNECTION_TIMEOUT);
        when(config.socketTimeout()).thenReturn(DEFAULT_SOCKET_TIMEOUT);
        when(config.userAgent()).thenReturn(TEST_USER_AGENT);

        fixture.activate(config);
    }
}