package com.webclient.lib.interceptor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static com.webclient.lib.util.HttpHeaders.CORRELATION_ID;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorrelationIdInterceptorTest {

    private CorrelationIdInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new CorrelationIdInterceptor();
    }

    @Test
    void intercept_noExistingHeader_addsCorrelationId() throws IOException {
        MockClientHttpRequest request = new MockClientHttpRequest(
                HttpMethod.GET, URI.create("https://example.com"));
        MockClientHttpResponse mockResponse = new MockClientHttpResponse(new byte[0], 200);
        ClientHttpRequestExecution execution = (req, body) -> mockResponse;

        interceptor.intercept(request, new byte[0], execution);

        String correlationId = request.getHeaders().getFirst(CORRELATION_ID);
        assertNotNull(correlationId);
        assertTrue(correlationId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    }

    @Test
    void intercept_existingHeader_preservesIt() throws IOException {
        String existingId = "existing-correlation-id";
        MockClientHttpRequest request = new MockClientHttpRequest(
                HttpMethod.GET, URI.create("https://example.com"));
        request.getHeaders().set(CORRELATION_ID, existingId);
        MockClientHttpResponse mockResponse = new MockClientHttpResponse(new byte[0], 200);
        ClientHttpRequestExecution execution = (req, body) -> mockResponse;

        interceptor.intercept(request, new byte[0], execution);

        assertEquals(existingId,
                request.getHeaders().getFirst(CORRELATION_ID));
    }
}
