package com.webclient.lib.model;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestServiceRequestTest {

    @Test
    void builder_withAllFields_buildsCorrectSpec() {
        Map<String, String> headers = Map.of("X-Custom", "value");
        Object body = "{\"key\":\"value\"}";

        RestServiceRequest<String> spec = RestServiceRequest.<String>builder()
                .url("https://example.com/api")
                .method(HttpMethod.POST)
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .acceptType(MediaType.APPLICATION_JSON)
                .body(body)
                .responseType(String.class)
                .build();

        assertEquals("https://example.com/api", spec.getUrl());
        assertEquals(HttpMethod.POST, spec.getHttpMethod());
        assertEquals("value", spec.getHeaders().get("X-Custom"));
        assertEquals(MediaType.APPLICATION_JSON, spec.getContentType());
        assertEquals(MediaType.APPLICATION_JSON, spec.getAcceptType());
        assertEquals(body, spec.getBody());
        assertEquals(String.class, spec.getResponseType());
    }

    @Test
    void builder_defaultMethod_isGet() {
        RestServiceRequest<String> spec = RestServiceRequest.<String>builder()
                .url("https://example.com")
                .responseType(String.class)
                .build();

        assertEquals(HttpMethod.GET, spec.getHttpMethod());
    }

    @Test
    void builder_noHeaders_returnsEmptyMap() {
        RestServiceRequest<String> spec = RestServiceRequest.<String>builder()
                .url("https://example.com")
                .responseType(String.class)
                .build();

        assertNotNull(spec.getHeaders());
        assertTrue(spec.getHeaders().isEmpty());
    }

    @Test
    void builder_headerMethod_addsIndividualHeaders() {
        RestServiceRequest<String> spec = RestServiceRequest.<String>builder()
                .url("https://example.com")
                .header("X-First", "one")
                .header("X-Second", "two")
                .responseType(String.class)
                .build();

        assertEquals("one", spec.getHeaders().get("X-First"));
        assertEquals("two", spec.getHeaders().get("X-Second"));
    }

    @Test
    void builder_headersAreImmutable() {
        RestServiceRequest<String> spec = RestServiceRequest.<String>builder()
                .url("https://example.com")
                .header("X-Key", "value")
                .responseType(String.class)
                .build();

        assertThrows(UnsupportedOperationException.class,
                () -> spec.getHeaders().put("X-New", "val"));
    }

    @Test
    void builder_optionalFieldsCanBeNull() {
        RestServiceRequest<String> spec = RestServiceRequest.<String>builder()
                .url("https://example.com")
                .responseType(String.class)
                .build();

        assertNull(spec.getContentType());
        assertNull(spec.getAcceptType());
        assertNull(spec.getBody());
    }

    @Test
    void builder_nullUrl_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> RestServiceRequest.<String>builder()
                        .responseType(String.class)
                        .build());
    }

    @Test
    void builder_blankUrl_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> RestServiceRequest.<String>builder()
                        .url("   ")
                        .responseType(String.class)
                        .build());
    }

    @Test
    void builder_nullResponseType_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> RestServiceRequest.<String>builder()
                        .url("https://example.com")
                        .build());
    }

    @Test
    void builder_defaultTimeoutAndRetry_areNull() {
        RestServiceRequest<String> spec = RestServiceRequest.<String>builder()
                .url("https://example.com")
                .responseType(String.class)
                .build();

        assertNull(spec.getTimeoutMs());
        assertNull(spec.getMaxRetries());
        assertNull(spec.getRetryIntervalMs());
    }

    @Test
    void builder_withPerRequestTimeout_setsValue() {
        RestServiceRequest<String> spec = RestServiceRequest.<String>builder()
                .url("https://example.com")
                .responseType(String.class)
                .timeoutMs(5000)
                .build();

        assertEquals(5000, spec.getTimeoutMs());
    }

    @Test
    void builder_withPerRequestRetry_setsValues() {
        RestServiceRequest<String> spec = RestServiceRequest.<String>builder()
                .url("https://example.com")
                .responseType(String.class)
                .maxRetries(5)
                .retryIntervalMs(2000L)
                .build();

        assertEquals(5, spec.getMaxRetries());
        assertEquals(2000L, spec.getRetryIntervalMs());
    }

    @Test
    void builder_defaultInterceptors_returnsEmptyList() {
        RestServiceRequest<String> spec = RestServiceRequest.<String>builder()
                .url("https://example.com")
                .responseType(String.class)
                .build();

        assertNotNull(spec.getInterceptors());
        assertTrue(spec.getInterceptors().isEmpty());
    }

    @Test
    void builder_withInterceptor_addsToList() {
        ClientHttpRequestInterceptor interceptor1 = (req, body, exec) -> exec.execute(req, body);
        ClientHttpRequestInterceptor interceptor2 = (req, body, exec) -> exec.execute(req, body);

        RestServiceRequest<String> spec = RestServiceRequest.<String>builder()
                .url("https://example.com")
                .responseType(String.class)
                .interceptor(interceptor1)
                .interceptor(interceptor2)
                .build();

        assertEquals(2, spec.getInterceptors().size());
        assertEquals(interceptor1, spec.getInterceptors().get(0));
        assertEquals(interceptor2, spec.getInterceptors().get(1));
    }

    @Test
    void builder_withInterceptors_setsList() {
        ClientHttpRequestInterceptor interceptor = (req, body, exec) -> exec.execute(req, body);

        RestServiceRequest<String> spec = RestServiceRequest.<String>builder()
                .url("https://example.com")
                .responseType(String.class)
                .interceptors(List.of(interceptor))
                .build();

        assertEquals(1, spec.getInterceptors().size());
    }

    @Test
    void builder_interceptorsAreImmutable() {
        ClientHttpRequestInterceptor interceptor = (req, body, exec) -> exec.execute(req, body);

        RestServiceRequest<String> spec = RestServiceRequest.<String>builder()
                .url("https://example.com")
                .responseType(String.class)
                .interceptor(interceptor)
                .build();

        assertThrows(UnsupportedOperationException.class,
                () -> spec.getInterceptors().add((req, body, exec) -> exec.execute(req, body)));
    }

    @Test
    void builder_allHttpMethods() {
        for (HttpMethod method : HttpMethod.values()) {
            RestServiceRequest<String> spec = RestServiceRequest.<String>builder()
                    .url("https://example.com")
                    .method(method)
                    .responseType(String.class)
                    .build();
            assertEquals(method, spec.getHttpMethod());
        }
    }
}
