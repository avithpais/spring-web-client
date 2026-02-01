package com.webclient.lib.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

/**
 * {@link ExchangeFilterFunction} that injects a bearer token into each
 * outgoing request.
 * <p>
 * The {@link BearerTokenProvider#getToken()} call is synchronous.
 * Implementations are expected to cache tokens and use concurrency primitives
 * (e.g., {@link java.util.concurrent.locks.StampedLock}) to guarantee
 * thread-safe, fast-path reads. The typical cost is a single volatile read
 * when the token is cached and valid.
 * <p>
 * On retries the filter is re-invoked, allowing the provider to return a
 * refreshed token if the previous one has expired.
 * <p>
 * If the request already contains an {@code Authorization} header (set
 * per-request on the {@code WebServiceRequest}), this filter preserves it.
 * <p>
 * If no {@link BearerTokenProvider} bean is registered, the filter is a no-op.
 */
@Order(200)
public class BearerTokenFilterFunction implements ExchangeFilterFunction {

    private BearerTokenProvider bearerTokenProvider;

    @Autowired(required = false)
    public void setBearerTokenProvider(BearerTokenProvider bearerTokenProvider) {
        this.bearerTokenProvider = bearerTokenProvider;
    }

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        if (bearerTokenProvider == null) {
            return next.exchange(request);
        }

        if (request.headers().getFirst("Authorization") != null) {
            return next.exchange(request);
        }

        String token = bearerTokenProvider.getToken();
        if (token == null || token.isBlank()) {
            return next.exchange(request);
        }

        ClientRequest authenticatedRequest = ClientRequest.from(request)
                .header("Authorization", "Bearer " + token)
                .build();
        return next.exchange(authenticatedRequest);
    }
}
