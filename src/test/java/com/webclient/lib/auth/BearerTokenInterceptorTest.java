package com.webclient.lib.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BearerTokenInterceptorTest {

    private BearerTokenInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new BearerTokenInterceptor();
    }

    @Test
    void intercept_noProvider_doesNotAddHeader() throws IOException {
        MockClientHttpRequest request = new MockClientHttpRequest(
                HttpMethod.GET, URI.create("https://example.com"));
        MockClientHttpResponse mockResponse = new MockClientHttpResponse(new byte[0], 200);
        ClientHttpRequestExecution execution = (req, body) -> mockResponse;

        interceptor.intercept(request, new byte[0], execution);

        assertNull(request.getHeaders().getFirst("Authorization"));
    }

    @Test
    void intercept_withProvider_addsAuthorizationHeader() throws IOException {
        interceptor.setBearerTokenProvider(() -> "test-token");

        MockClientHttpRequest request = new MockClientHttpRequest(
                HttpMethod.GET, URI.create("https://example.com"));
        MockClientHttpResponse mockResponse = new MockClientHttpResponse(new byte[0], 200);
        ClientHttpRequestExecution execution = (req, body) -> mockResponse;

        interceptor.intercept(request, new byte[0], execution);

        assertEquals("Bearer test-token", request.getHeaders().getFirst("Authorization"));
    }

    @Test
    void intercept_existingAuthHeader_preservesIt() throws IOException {
        interceptor.setBearerTokenProvider(() -> "test-token");

        MockClientHttpRequest request = new MockClientHttpRequest(
                HttpMethod.GET, URI.create("https://example.com"));
        request.getHeaders().set("Authorization", "Basic abc123");
        MockClientHttpResponse mockResponse = new MockClientHttpResponse(new byte[0], 200);
        ClientHttpRequestExecution execution = (req, body) -> mockResponse;

        interceptor.intercept(request, new byte[0], execution);

        assertEquals("Basic abc123", request.getHeaders().getFirst("Authorization"));
    }

    @Test
    void intercept_nullToken_doesNotAddHeader() throws IOException {
        interceptor.setBearerTokenProvider(() -> null);

        MockClientHttpRequest request = new MockClientHttpRequest(
                HttpMethod.GET, URI.create("https://example.com"));
        MockClientHttpResponse mockResponse = new MockClientHttpResponse(new byte[0], 200);
        ClientHttpRequestExecution execution = (req, body) -> mockResponse;

        interceptor.intercept(request, new byte[0], execution);

        assertNull(request.getHeaders().getFirst("Authorization"));
    }

    @Test
    void intercept_blankToken_doesNotAddHeader() throws IOException {
        interceptor.setBearerTokenProvider(() -> "   ");

        MockClientHttpRequest request = new MockClientHttpRequest(
                HttpMethod.GET, URI.create("https://example.com"));
        MockClientHttpResponse mockResponse = new MockClientHttpResponse(new byte[0], 200);
        ClientHttpRequestExecution execution = (req, body) -> mockResponse;

        interceptor.intercept(request, new byte[0], execution);

        assertNull(request.getHeaders().getFirst("Authorization"));
    }
}
