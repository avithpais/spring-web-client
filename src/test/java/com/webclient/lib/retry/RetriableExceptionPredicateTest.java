package com.webclient.lib.retry;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetriableExceptionPredicateTest {

    private final RetriableExceptionPredicate predicate = RetriableExceptionPredicate.INSTANCE;

    @Test
    void serverError500_isRetriable() {
        var ex = WebClientResponseException.create(500, "Internal Server Error",
                null, null, null);
        assertTrue(predicate.test(ex));
    }

    @Test
    void serverError502_isRetriable() {
        var ex = WebClientResponseException.create(502, "Bad Gateway",
                null, null, null);
        assertTrue(predicate.test(ex));
    }

    @Test
    void serverError503_isRetriable() {
        var ex = WebClientResponseException.create(503, "Service Unavailable",
                null, null, null);
        assertTrue(predicate.test(ex));
    }

    @Test
    void tooManyRequests429_isRetriable() {
        var ex = WebClientResponseException.create(429, "Too Many Requests",
                null, null, null);
        assertTrue(predicate.test(ex));
    }

    @Test
    void clientError400_isNotRetriable() {
        var ex = WebClientResponseException.create(400, "Bad Request",
                null, null, null);
        assertFalse(predicate.test(ex));
    }

    @Test
    void clientError401_isNotRetriable() {
        var ex = WebClientResponseException.create(401, "Unauthorized",
                null, null, null);
        assertFalse(predicate.test(ex));
    }

    @Test
    void clientError404_isNotRetriable() {
        var ex = WebClientResponseException.create(404, "Not Found",
                null, null, null);
        assertFalse(predicate.test(ex));
    }

    @Test
    void webClientRequestException_isRetriable() {
        var cause = new ConnectException("Connection refused");
        var ex = new WebClientRequestException(
                cause, HttpMethod.GET, URI.create("https://example.com"), new HttpHeaders());
        assertTrue(predicate.test(ex));
    }

    @Test
    void ioException_isRetriable() {
        assertTrue(predicate.test(new IOException("Network unreachable")));
    }

    @Test
    void connectException_isRetriable() {
        assertTrue(predicate.test(new ConnectException("Connection refused")));
    }

    @Test
    void runtimeException_isNotRetriable() {
        assertFalse(predicate.test(new RuntimeException("unexpected")));
    }

    @Test
    void illegalArgumentException_isNotRetriable() {
        assertFalse(predicate.test(new IllegalArgumentException("bad input")));
    }

    @Test
    void nullPointerException_isNotRetriable() {
        assertFalse(predicate.test(new NullPointerException("oops")));
    }
}
