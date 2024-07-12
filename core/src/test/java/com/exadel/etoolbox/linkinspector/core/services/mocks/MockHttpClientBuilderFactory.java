package com.exadel.etoolbox.linkinspector.core.services.mocks;

import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Map;

public class MockHttpClientBuilderFactory implements HttpClientBuilderFactory {

    public static final String PN_CLIENT = "client";
    public static final String PN_STATUS_CODE = "statusCode";
    public static final String PN_STATUS_MESSAGE = "statusMessage";

    private HttpClientBuilder httpClientBuilder;

    @Override
    public HttpClientBuilder newBuilder() {
        return httpClientBuilder;
    }

    private void activate(Map<String, Object> properties) throws IOException {
        CloseableHttpClient client = (CloseableHttpClient) properties.getOrDefault(PN_CLIENT, null);
        if (client == null) {
            int statusCode = (int) properties.getOrDefault(PN_STATUS_CODE, HttpStatus.SC_NOT_FOUND);
            String statusMessage = (String) properties.getOrDefault(PN_STATUS_MESSAGE, "Not Found");
            StatusLine statusLine = new BasicStatusLine(HttpVersion.HTTP_1_0, statusCode, statusMessage);

            CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
            Mockito.when(response.getStatusLine()).thenReturn(statusLine);

            client = Mockito.mock(CloseableHttpClient.class);
            Mockito.when(client.execute(Mockito.any())).thenReturn(response);
        }
        httpClientBuilder = Mockito.mock(HttpClientBuilder.class);
        Mockito.when(httpClientBuilder.setDefaultRequestConfig(Mockito.any())).thenReturn(httpClientBuilder);
        Mockito.when(httpClientBuilder.build()).thenReturn(client);
    }
}
