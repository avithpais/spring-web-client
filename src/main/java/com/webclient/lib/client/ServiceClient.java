package com.webclient.lib.client;

import com.webclient.lib.model.WebServiceRequest;
import reactor.core.publisher.Mono;

/**
 * Client interface for executing HTTP calls to downstream services.
 * <p>
 * Implementations handle connection management, SSL, retry logic, timeout
 * enforcement, and bearer-token injection.
 */
public interface ServiceClient {

    /**
     * Executes an HTTP request described by the given specification.
     *
     * @param request the request configuration including URL, method, headers,
     *                body, response type, and optional per-request timeout/retry overrides
     * @param <T>     the expected response body type
     * @return a {@link Mono} emitting the deserialized response body
     */
    <T> Mono<T> execute(WebServiceRequest<T> request);
}
