package com.webclient.lib.interceptor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestLoggingInterceptorTest {

    private RequestLoggingInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new RequestLoggingInterceptor();
    }

    @Test
    void intercept_successfulRequest_returnsResponse() throws IOException {
        MockClientHttpRequest request = new MockClientHttpRequest(
                HttpMethod.GET, URI.create("https://example.com"));
        MockClientHttpResponse mockResponse = new MockClientHttpResponse(new byte[0], 200);
        ClientHttpRequestExecution execution = (req, body) -> mockResponse;

        var response = interceptor.intercept(request, new byte[0], execution);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void intercept_failedRequest_propagatesException() {
        MockClientHttpRequest request = new MockClientHttpRequest(
                HttpMethod.GET, URI.create("https://example.com"));
        ClientHttpRequestExecution execution = (req, body) -> {
            throw new IOException("Connection refused");
        };

        assertThrows(IOException.class,
                () -> interceptor.intercept(request, new byte[0], execution));
    }
}
