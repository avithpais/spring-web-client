package com.webclient.lib.client;

import com.webclient.lib.config.HttpClientProperties;
import com.webclient.lib.model.RestServiceRequest;
import com.webclient.lib.retry.SyncRetryExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.function.Supplier;

/**
 * Synchronous HTTP client backed by Spring {@link RestClient}.
 * <p>
 * This is the synchronous counterpart to {@link WebServiceClient}.
 * Responsibilities are split into focused private methods:
 * <ul>
 *   <li>{@link #resolveRestClient} &mdash; applies per-request
 *       {@link ClientHttpRequestInterceptor} interceptors via {@code restClient.mutate()}.</li>
 *   <li>{@link #buildRequestSpec} &mdash; translates a {@link RestServiceRequest}
 *       into a RestClient request specification.</li>
 *   <li>{@link #executeWithRetry} &mdash; selects per-request or global retry.</li>
 * </ul>
 * <p>
 * Interceptors (bearer token, correlation ID, logging, etc.) are <b>not</b>
 * auto-registered on the shared RestClient.  Instead, each
 * {@link RestServiceRequest} declares its own interceptor list via the builder's
 * {@code interceptor()} method, giving callers full control over which interceptors
 * run for each call.
 */
public class RestServiceClient {

    private RestClient restClient;
    private SyncRetryExecutor syncRetryExecutor;
    private HttpClientProperties properties;

    @Autowired
    public void setRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Autowired
    public void setSyncRetryExecutor(SyncRetryExecutor syncRetryExecutor) {
        this.syncRetryExecutor = syncRetryExecutor;
    }

    @Autowired
    public void setProperties(HttpClientProperties properties) {
        this.properties = properties;
    }

    // ------------------------------------------------------------------ //
    //  Public API
    // ------------------------------------------------------------------ //

    public <T> T execute(RestServiceRequest<T> request) {
        RestClient effectiveClient = resolveRestClient(request);

        return executeWithRetry(() -> {
            RestClient.RequestHeadersSpec<?> spec = buildRequestSpec(effectiveClient, request);
            return spec.retrieve().body(request.getResponseType());
        }, request);
    }

    // ------------------------------------------------------------------ //
    //  Per-request interceptor resolution
    // ------------------------------------------------------------------ //

    private RestClient resolveRestClient(RestServiceRequest<?> request) {
        List<ClientHttpRequestInterceptor> interceptors = request.getInterceptors();
        if (interceptors == null || interceptors.isEmpty()) {
            return this.restClient;
        }
        RestClient.Builder builder = this.restClient.mutate();
        interceptors.forEach(builder::requestInterceptor);
        return builder.build();
    }

    // ------------------------------------------------------------------ //
    //  Request building
    // ------------------------------------------------------------------ //

    private <T> RestClient.RequestHeadersSpec<?> buildRequestSpec(
            RestClient client, RestServiceRequest<T> request) {

        RestClient.RequestBodySpec requestSpec = client
                .method(request.getHttpMethod())
                .uri(request.getUrl());

        if (!request.getHeaders().isEmpty()) {
            requestSpec.headers(h -> request.getHeaders().forEach(h::set));
        }

        if (request.getContentType() != null) {
            requestSpec.contentType(request.getContentType());
        }

        if (request.getAcceptType() != null) {
            requestSpec.accept(request.getAcceptType());
        }

        return request.getBody() != null
                ? requestSpec.body(request.getBody())
                : requestSpec;
    }

    // ------------------------------------------------------------------ //
    //  Retry
    // ------------------------------------------------------------------ //

    private <T> T executeWithRetry(Supplier<T> operation, RestServiceRequest<T> request) {
        int maxRetries = request.getMaxRetries() != null
                ? request.getMaxRetries()
                : properties.getRetry().getMaxRetries();

        if (maxRetries == 0) {
            return operation.get();
        }

        long retryIntervalMs = request.getRetryIntervalMs() != null
                ? request.getRetryIntervalMs()
                : properties.getRetry().getRetryIntervalMs();

        return syncRetryExecutor.executeWithRetry(operation, maxRetries, retryIntervalMs);
    }
}
