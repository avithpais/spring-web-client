package com.webclient.lib.retry;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyncRetriableExceptionPredicateTest {

    private final SyncRetriableExceptionPredicate predicate = SyncRetriableExceptionPredicate.INSTANCE;

    // ------------------------------------------------------------------ //
    //  HTTP 5xx (Server Errors) — retriable
    // ------------------------------------------------------------------ //

    @Test
    void test_serverError500_returnsTrue() {
        HttpServerErrorException exception = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        assertTrue(predicate.test(exception));
    }

    @Test
    void test_serverError502_returnsTrue() {
        HttpServerErrorException exception = new HttpServerErrorException(HttpStatus.BAD_GATEWAY);
        assertTrue(predicate.test(exception));
    }

    @Test
    void test_serverError503_returnsTrue() {
        HttpServerErrorException exception = new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE);
        assertTrue(predicate.test(exception));
    }

    @Test
    void test_serverError504_returnsTrue() {
        HttpServerErrorException exception = new HttpServerErrorException(HttpStatus.GATEWAY_TIMEOUT);
        assertTrue(predicate.test(exception));
    }

    // ------------------------------------------------------------------ //
    //  HTTP 429 (Rate Limiting) — retriable
    // ------------------------------------------------------------------ //

    @Test
    void test_tooManyRequests429_returnsTrue() {
        HttpClientErrorException exception = new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS);
        assertTrue(predicate.test(exception));
    }

    // ------------------------------------------------------------------ //
    //  HTTP 4xx (Client Errors except 429) — NOT retriable
    // ------------------------------------------------------------------ //

    @Test
    void test_badRequest400_returnsFalse() {
        HttpClientErrorException exception = new HttpClientErrorException(HttpStatus.BAD_REQUEST);
        assertFalse(predicate.test(exception));
    }

    @Test
    void test_unauthorized401_returnsFalse() {
        HttpClientErrorException exception = new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
        assertFalse(predicate.test(exception));
    }

    @Test
    void test_forbidden403_returnsFalse() {
        HttpClientErrorException exception = new HttpClientErrorException(HttpStatus.FORBIDDEN);
        assertFalse(predicate.test(exception));
    }

    @Test
    void test_notFound404_returnsFalse() {
        HttpClientErrorException exception = new HttpClientErrorException(HttpStatus.NOT_FOUND);
        assertFalse(predicate.test(exception));
    }

    // ------------------------------------------------------------------ //
    //  ResourceAccessException (Connection Issues) — retriable
    // ------------------------------------------------------------------ //

    @Test
    void test_resourceAccessException_returnsTrue() {
        ResourceAccessException exception = new ResourceAccessException("Connection refused");
        assertTrue(predicate.test(exception));
    }

    @Test
    void test_resourceAccessExceptionWithCause_returnsTrue() {
        ResourceAccessException exception = new ResourceAccessException(
                "Connection timed out", new ConnectException("Connection refused"));
        assertTrue(predicate.test(exception));
    }

    // ------------------------------------------------------------------ //
    //  IOException — retriable
    // ------------------------------------------------------------------ //

    @Test
    void test_ioException_returnsTrue() {
        IOException exception = new IOException("Network error");
        assertTrue(predicate.test(exception));
    }

    @Test
    void test_connectException_returnsTrue() {
        ConnectException exception = new ConnectException("Connection refused");
        assertTrue(predicate.test(exception));
    }

    @Test
    void test_socketTimeoutException_returnsTrue() {
        SocketTimeoutException exception = new SocketTimeoutException("Read timed out");
        assertTrue(predicate.test(exception));
    }

    // ------------------------------------------------------------------ //
    //  Other Exceptions — NOT retriable
    // ------------------------------------------------------------------ //

    @Test
    void test_runtimeException_returnsFalse() {
        RuntimeException exception = new RuntimeException("Unexpected error");
        assertFalse(predicate.test(exception));
    }

    @Test
    void test_illegalArgumentException_returnsFalse() {
        IllegalArgumentException exception = new IllegalArgumentException("Invalid input");
        assertFalse(predicate.test(exception));
    }

    @Test
    void test_nullPointerException_returnsFalse() {
        NullPointerException exception = new NullPointerException("null");
        assertFalse(predicate.test(exception));
    }
}
