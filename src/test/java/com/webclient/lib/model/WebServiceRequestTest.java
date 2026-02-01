package com.webclient.lib.model;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebServiceRequestTest {

    @Test
    void builder_withAllFields_buildsCorrectSpec() {
        Map<String, String> headers = Map.of("X-Custom", "value");
        Object body = "{\"key\":\"value\"}";

        WebServiceRequest<String> spec = WebServiceRequest.<String>builder()
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
        WebServiceRequest<String> spec = WebServiceRequest.<String>builder()
                .url("https://example.com")
                .responseType(String.class)
                .build();

        assertEquals(HttpMethod.GET, spec.getHttpMethod());
    }

    @Test
    void builder_noHeaders_returnsEmptyMap() {
        WebServiceRequest<String> spec = WebServiceRequest.<String>builder()
                .url("https://example.com")
                .responseType(String.class)
                .build();

        assertNotNull(spec.getHeaders());
        assertTrue(spec.getHeaders().isEmpty());
    }

    @Test
    void builder_headerMethod_addsIndividualHeaders() {
        WebServiceRequest<String> spec = WebServiceRequest.<String>builder()
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
        WebServiceRequest<String> spec = WebServiceRequest.<String>builder()
                .url("https://example.com")
                .header("X-Key", "value")
                .responseType(String.class)
                .build();

        assertThrows(UnsupportedOperationException.class,
                () -> spec.getHeaders().put("X-New", "val"));
    }

    @Test
    void builder_optionalFieldsCanBeNull() {
        WebServiceRequest<String> spec = WebServiceRequest.<String>builder()
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
                () -> WebServiceRequest.<String>builder()
                        .responseType(String.class)
                        .build());
    }

    @Test
    void builder_blankUrl_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> WebServiceRequest.<String>builder()
                        .url("   ")
                        .responseType(String.class)
                        .build());
    }

    @Test
    void builder_nullResponseType_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> WebServiceRequest.<String>builder()
                        .url("https://example.com")
                        .build());
    }

    @Test
    void builder_defaultTimeoutAndRetry_areNull() {
        WebServiceRequest<String> spec = WebServiceRequest.<String>builder()
                .url("https://example.com")
                .responseType(String.class)
                .build();

        assertNull(spec.getTimeoutMs());
        assertNull(spec.getMaxRetries());
        assertNull(spec.getRetryIntervalMs());
    }

    @Test
    void builder_withPerRequestTimeout_setsValue() {
        WebServiceRequest<String> spec = WebServiceRequest.<String>builder()
                .url("https://example.com")
                .responseType(String.class)
                .timeoutMs(5000)
                .build();

        assertEquals(5000, spec.getTimeoutMs());
    }

    @Test
    void builder_withPerRequestRetry_setsValues() {
        WebServiceRequest<String> spec = WebServiceRequest.<String>builder()
                .url("https://example.com")
                .responseType(String.class)
                .maxRetries(5)
                .retryIntervalMs(2000L)
                .build();

        assertEquals(5, spec.getMaxRetries());
        assertEquals(2000L, spec.getRetryIntervalMs());
    }

    @Test
    void builder_defaultFilters_returnsEmptyList() {
        WebServiceRequest<String> spec = WebServiceRequest.<String>builder()
                .url("https://example.com")
                .responseType(String.class)
                .build();

        assertNotNull(spec.getFilters());
        assertTrue(spec.getFilters().isEmpty());
    }

    @Test
    void builder_withFilter_addsToList() {
        ExchangeFilterFunction filter1 = (req, next) -> next.exchange(req);
        ExchangeFilterFunction filter2 = (req, next) -> next.exchange(req);

        WebServiceRequest<String> spec = WebServiceRequest.<String>builder()
                .url("https://example.com")
                .responseType(String.class)
                .filter(filter1)
                .filter(filter2)
                .build();

        assertEquals(2, spec.getFilters().size());
        assertEquals(filter1, spec.getFilters().get(0));
        assertEquals(filter2, spec.getFilters().get(1));
    }

    @Test
    void builder_withFilters_setsList() {
        ExchangeFilterFunction filter = (req, next) -> next.exchange(req);

        WebServiceRequest<String> spec = WebServiceRequest.<String>builder()
                .url("https://example.com")
                .responseType(String.class)
                .filters(List.of(filter))
                .build();

        assertEquals(1, spec.getFilters().size());
    }

    @Test
    void builder_filtersAreImmutable() {
        ExchangeFilterFunction filter = (req, next) -> next.exchange(req);

        WebServiceRequest<String> spec = WebServiceRequest.<String>builder()
                .url("https://example.com")
                .responseType(String.class)
                .filter(filter)
                .build();

        assertThrows(UnsupportedOperationException.class,
                () -> spec.getFilters().add((req, next) -> next.exchange(req)));
    }

    @Test
    void builder_allHttpMethods() {
        for (HttpMethod method : HttpMethod.values()) {
            WebServiceRequest<String> spec = WebServiceRequest.<String>builder()
                    .url("https://example.com")
                    .method(method)
                    .responseType(String.class)
                    .build();
            assertEquals(method, spec.getHttpMethod());
        }
    }
}
