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
import com.exadel.etoolbox.linkinspector.core.models.LinkResult;
import com.exadel.etoolbox.linkinspector.core.services.mocks.MockHttpClientBuilderFactory;
import com.exadel.etoolbox.linkinspector.core.services.mocks.MockRepositoryHelper;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(AemContextExtension.class)
class ExternalResultResolverImplTest {

    private static final Map<String, Object> HTTP_PARAMS;

    static {
        Map<String, Object> params = new HashMap<>();
        params.put("connectionTimeout", 5000);
        params.put("socketTimeout", 15000);
        params.put("userAgent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36");
        HTTP_PARAMS = Collections.unmodifiableMap(params);
    }

    private final AemContext context = new AemContext();

    @Test
    void testCheckLink_nullResponse() throws IOException {
        CloseableHttpClient client = mock(CloseableHttpClient.class);
        when(client.execute(any())).thenReturn(null);
        context.registerInjectActivateService(
                new MockHttpClientBuilderFactory(),
                Collections.singletonMap(MockHttpClientBuilderFactory.PN_CLIENT, client));

        Result testResult = getTestLink();
        context.registerInjectActivateService(new MockRepositoryHelper(context.resourceResolver()));
        context.registerInjectActivateService(new ExternalLinkResolverImpl(), HTTP_PARAMS).validate(testResult, context.resourceResolver());
        assertEquals(HttpStatus.SC_BAD_REQUEST, testResult.getStatus().getCode());
    }

    @Test
    void testCheckLink_returnStatusCode404whenAll404() throws IOException {
        CloseableHttpResponse response404 = mock(CloseableHttpResponse.class);
        when(response404.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_0, HttpStatus.SC_NOT_FOUND, "Not Found"));

        CloseableHttpClient client = mock(CloseableHttpClient.class);
        when(client.execute(any(HttpHead.class))).thenReturn(response404);
        when(client.execute(any(HttpGet.class))).thenReturn(response404);
        context.registerInjectActivateService(
                new MockHttpClientBuilderFactory(),
                Collections.singletonMap(MockHttpClientBuilderFactory.PN_CLIENT, client));

        Result testResult = getTestLink();
        context.registerInjectActivateService(new MockRepositoryHelper(context.resourceResolver()));
        context.registerInjectActivateService(new ExternalLinkResolverImpl(), HTTP_PARAMS).validate(testResult, context.resourceResolver());
        assertEquals(HttpStatus.SC_NOT_FOUND, testResult.getStatus().getCode());
    }

    @Test
    void testCheckLink_returnStatusCode200when404Head200Get() throws IOException {
        CloseableHttpResponse response404 = mock(CloseableHttpResponse.class);
        when(response404.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_0, HttpStatus.SC_NOT_FOUND, "Not Found"));

        CloseableHttpResponse response200 = mock(CloseableHttpResponse.class);
        when(response200.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_0, HttpStatus.SC_OK, "OK"));

        CloseableHttpClient client = mock(CloseableHttpClient.class);
        when(client.execute(any(HttpHead.class))).thenReturn(response404);
        when(client.execute(any(HttpGet.class))).thenReturn(response200);
        context.registerInjectActivateService(
                new MockHttpClientBuilderFactory(),
                Collections.singletonMap(MockHttpClientBuilderFactory.PN_CLIENT, client));

        Result testResult = getTestLink();
        context.registerInjectActivateService(new MockRepositoryHelper(context.resourceResolver()));
        context.registerInjectActivateService(new ExternalLinkResolverImpl(), HTTP_PARAMS).validate(testResult, context.resourceResolver());
        assertEquals(HttpStatus.SC_OK, testResult.getStatus().getCode());
    }

    @Test
    void testCheckLink_returnStatusCode200when200Head() {
        context.registerInjectActivateService(
                new MockHttpClientBuilderFactory(),
                new HashMap<String, Object>() {{
                    put(MockHttpClientBuilderFactory.PN_STATUS_CODE, HttpStatus.SC_OK);
                    put(MockHttpClientBuilderFactory.PN_STATUS_MESSAGE, "OK");
                }});

        Result testResult = getTestLink();
        context.registerInjectActivateService(new MockRepositoryHelper(context.resourceResolver()));
        context.registerInjectActivateService(new ExternalLinkResolverImpl(), HTTP_PARAMS).validate(testResult, context.resourceResolver());
        assertEquals(HttpStatus.SC_OK, testResult.getStatus().getCode());
    }

    @Test
    void testDeactivate() throws IOException {
        CloseableHttpClient client = mock(CloseableHttpClient.class);
        context.registerInjectActivateService(
                new MockHttpClientBuilderFactory(),
                Collections.singletonMap(MockHttpClientBuilderFactory.PN_CLIENT, client));
        context.registerInjectActivateService(new MockRepositoryHelper(context.resourceResolver()));
        ExternalLinkResolverImpl fixture = context.registerInjectActivateService(new ExternalLinkResolverImpl(), HTTP_PARAMS);
        fixture.deactivate();
        verify(client).close();
    }

    private static Result getTestLink() {
        return new LinkResult("External", "https://www.google.com/test.html");
    }
}