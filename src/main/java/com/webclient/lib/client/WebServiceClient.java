package com.webclient.lib.client;

import com.webclient.lib.config.HttpClientProperties;
import com.webclient.lib.model.WebServiceRequest;
import com.webclient.lib.retry.RetryStrategyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * {@link ServiceClient} implementation backed by Spring {@link WebClient}.
 * <p>
 * Responsibilities are split into focused private methods:
 * <ul>
 *   <li>{@link #resolveWebClient} &mdash; applies per-request
 *       {@link ExchangeFilterFunction} filters via {@code webClient.mutate()}.</li>
 *   <li>{@link #buildRequestSpec} &mdash; translates a {@link WebServiceRequest}
 *       into a WebClient request specification.</li>
 *   <li>{@link #handleResponse} &mdash; maps the raw HTTP response to the
 *       target type or propagates an error.</li>
 *   <li>{@link #applyRetry} &mdash; selects per-request or global retry.</li>
 *   <li>{@link #applyTimeout} &mdash; selects per-request or global timeout.</li>
 * </ul>
 * <p>
 * Filters (bearer token, correlation ID, logging, etc.) are <b>not</b>
 * auto-registered on the shared WebClient.  Instead, each
 * {@link WebServiceRequest} declares its own filter list via the builder's
 * {@code filter()} method, giving callers full control over which filters
 * run for each call.
 */
public class WebServiceClient implements ServiceClient {

    private WebClient webClient;
    private RetryStrategyFactory retryStrategyFactory;
    private HttpClientProperties properties;

    @Autowired
    public void setWebClient(WebClient webClient) {
        this.webClient = webClient;
    }

    @Autowired
    public void setRetryStrategyFactory(RetryStrategyFactory retryStrategyFactory) {
        this.retryStrategyFactory = retryStrategyFactory;
    }

    @Autowired
    public void setProperties(HttpClientProperties properties) {
        this.properties = properties;
    }

    // ------------------------------------------------------------------ //
    //  Public API
    // ------------------------------------------------------------------ //

    @Override
    public <T> Mono<T> execute(WebServiceRequest<T> request) {
        WebClient effectiveClient = resolveWebClient(request);
        WebClient.RequestHeadersSpec<?> spec = buildRequestSpec(effectiveClient, request);

        Mono<T> result = spec.exchangeToMono(response ->
                handleResponse(response, request.getResponseType()));

        result = applyRetry(result, request);
        result = applyTimeout(result, request);

        return result;
    }

    // ------------------------------------------------------------------ //
    //  Per-request filter resolution
    // ------------------------------------------------------------------ //

    private WebClient resolveWebClient(WebServiceRequest<?> request) {
        List<ExchangeFilterFunction> filters = request.getFilters();
        if (filters == null || filters.isEmpty()) {
            return this.webClient;
        }
        WebClient.Builder builder = this.webClient.mutate();
        filters.forEach(builder::filter);
        return builder.build();
    }

    // ------------------------------------------------------------------ //
    //  Request building
    // ------------------------------------------------------------------ //

    private <T> WebClient.RequestHeadersSpec<?> buildRequestSpec(
            WebClient client, WebServiceRequest<T> request) {

        WebClient.RequestBodySpec requestSpec = client
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
                ? requestSpec.bodyValue(request.getBody())
                : requestSpec;
    }

    // ------------------------------------------------------------------ //
    //  Response handling
    // ------------------------------------------------------------------ //

    private <T> Mono<T> handleResponse(ClientResponse response, Class<T> responseType) {
        if (response.statusCode().value() == 200) {
            return response.bodyToMono(responseType);
        }
        return response.createException().flatMap(Mono::error);
    }

    // ------------------------------------------------------------------ //
    //  Retry
    // ------------------------------------------------------------------ //

    private <T> Mono<T> applyRetry(Mono<T> result, WebServiceRequest<T> request) {
        if (request.getMaxRetries() != null) {
            if (request.getMaxRetries() == 0) {
                return result;
            }
            long intervalMs = request.getRetryIntervalMs() != null
                    ? request.getRetryIntervalMs()
                    : properties.getRetry().getRetryIntervalMs();
            return result.retryWhen(
                    retryStrategyFactory.createRetrySpec(request.getMaxRetries(), intervalMs));
        }

        if (request.getRetryIntervalMs() != null) {
            return result.retryWhen(retryStrategyFactory.createRetrySpec(
                    properties.getRetry().getMaxRetries(), request.getRetryIntervalMs()));
        }

        return result.retryWhen(retryStrategyFactory.createRetrySpec());
    }

    // ------------------------------------------------------------------ //
    //  Timeout
    // ------------------------------------------------------------------ //

    private <T> Mono<T> applyTimeout(Mono<T> result, WebServiceRequest<T> request) {
        int effectiveTimeout = request.getTimeoutMs() != null
                ? request.getTimeoutMs()
                : properties.getTimeout().getTimeoutMs();
        return result.timeout(Duration.ofMillis(effectiveTimeout));
    }
}
