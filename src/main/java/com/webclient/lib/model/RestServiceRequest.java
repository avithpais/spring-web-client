package com.webclient.lib.model;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable specification for a downstream service call using synchronous RestClient.
 * Use the {@link Builder} to construct instances.
 * <p>
 * This is the synchronous equivalent of {@link WebServiceRequest}.
 * Uses {@link ClientHttpRequestInterceptor} instead of {@code ExchangeFilterFunction}.
 *
 * @param <T> the expected response body type
 */
public class RestServiceRequest<T> {

    private final String url;
    private final HttpMethod httpMethod;
    private final Map<String, String> headers;
    private final MediaType contentType;
    private final MediaType acceptType;
    private final Object body;
    private final Class<T> responseType;
    private final Integer timeoutMs;
    private final Integer maxRetries;
    private final Long retryIntervalMs;
    private final List<ClientHttpRequestInterceptor> interceptors;

    private RestServiceRequest(Builder<T> builder) {
        this.url = builder.url;
        this.httpMethod = builder.httpMethod;
        this.headers = builder.headers != null
                ? Collections.unmodifiableMap(new HashMap<>(builder.headers))
                : Collections.emptyMap();
        this.contentType = builder.contentType;
        this.acceptType = builder.acceptType;
        this.body = builder.body;
        this.responseType = builder.responseType;
        this.timeoutMs = builder.timeoutMs;
        this.maxRetries = builder.maxRetries;
        this.retryIntervalMs = builder.retryIntervalMs;
        this.interceptors = builder.interceptors != null
                ? Collections.unmodifiableList(new ArrayList<>(builder.interceptors))
                : Collections.emptyList();
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public String getUrl() {
        return url;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public MediaType getContentType() {
        return contentType;
    }

    public MediaType getAcceptType() {
        return acceptType;
    }

    public Object getBody() {
        return body;
    }

    public Class<T> getResponseType() {
        return responseType;
    }

    public Integer getTimeoutMs() {
        return timeoutMs;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public Long getRetryIntervalMs() {
        return retryIntervalMs;
    }

    public List<ClientHttpRequestInterceptor> getInterceptors() {
        return interceptors;
    }

    public static class Builder<T> {

        private String url;
        private HttpMethod httpMethod = HttpMethod.GET;
        private Map<String, String> headers;
        private MediaType contentType;
        private MediaType acceptType;
        private Object body;
        private Class<T> responseType;
        private Integer timeoutMs;
        private Integer maxRetries;
        private Long retryIntervalMs;
        private List<ClientHttpRequestInterceptor> interceptors;

        public Builder<T> url(String url) {
            this.url = url;
            return this;
        }

        public Builder<T> method(HttpMethod httpMethod) {
            this.httpMethod = httpMethod;
            return this;
        }

        public Builder<T> headers(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public Builder<T> header(String name, String value) {
            if (this.headers == null) {
                this.headers = new HashMap<>();
            }
            this.headers.put(name, value);
            return this;
        }

        public Builder<T> contentType(MediaType contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder<T> acceptType(MediaType acceptType) {
            this.acceptType = acceptType;
            return this;
        }

        public Builder<T> body(Object body) {
            this.body = body;
            return this;
        }

        public Builder<T> responseType(Class<T> responseType) {
            this.responseType = responseType;
            return this;
        }

        public Builder<T> timeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        public Builder<T> maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder<T> retryIntervalMs(long retryIntervalMs) {
            this.retryIntervalMs = retryIntervalMs;
            return this;
        }

        public Builder<T> interceptor(ClientHttpRequestInterceptor interceptor) {
            if (this.interceptors == null) {
                this.interceptors = new ArrayList<>();
            }
            this.interceptors.add(interceptor);
            return this;
        }

        public Builder<T> interceptors(List<ClientHttpRequestInterceptor> interceptors) {
            this.interceptors = new ArrayList<>(interceptors);
            return this;
        }

        public RestServiceRequest<T> build() {
            if (url == null || url.isBlank()) {
                throw new IllegalArgumentException("URL must not be blank");
            }
            if (responseType == null) {
                throw new IllegalArgumentException("responseType must not be null");
            }
            return new RestServiceRequest<>(this);
        }
    }
}
