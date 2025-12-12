package com.toolshed.backend.service;

import java.io.IOException;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.Timeout;
import org.springframework.stereotype.Component;

/**
 * Basic HTTP client implementation using Apache HttpClient.
 * Provides a simple wrapper around Apache's CloseableHttpClient
 * for executing HTTP GET requests and returning response content as strings.
 */
@Component
public class TqsBasicHttpClient implements ISimpleHttpClient {

    @Override
    public String doHttpGet(String url) throws IOException {
        // Add conservative timeouts so external GeoAPI calls cannot hang the UI
        // Keep the backend cap below the frontend timeout so the UI can react (and fall back)
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(Timeout.ofSeconds(10))
            .setResponseTimeout(Timeout.ofSeconds(10))
            .build();

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            request.setConfig(requestConfig);

            try (CloseableHttpResponse response = client.execute(request)) {
                HttpEntity entity = response.getEntity();
                return EntityUtils.toString(entity);
            } catch (org.apache.hc.core5.http.ParseException e) {
                throw new IOException("Failed to parse response", e);
            }
        }
    }
}
