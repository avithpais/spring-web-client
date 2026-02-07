package com.webclient.lib.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

import static com.webclient.lib.util.HttpHeaders.AUTHORIZATION;

/**
 * {@link ClientHttpRequestInterceptor} that injects a bearer token into each
 * outgoing request.
 * <p>
 * The {@link BearerTokenProvider#getToken()} call is synchronous.
 * Implementations are expected to cache tokens and use concurrency primitives
 * (e.g., {@link java.util.concurrent.locks.StampedLock}) to guarantee
 * thread-safe, fast-path reads. The typical cost is a single volatile read
 * when the token is cached and valid.
 * <p>
 * On retries the interceptor is re-invoked, allowing the provider to return a
 * refreshed token if the previous one has expired.
 * <p>
 * If the request already contains an {@code Authorization} header (set
 * per-request on the {@code RestServiceRequest}), this interceptor preserves it.
 * <p>
 * If no {@link BearerTokenProvider} bean is registered, the interceptor is a no-op.
 * <p>
 * This is the synchronous RestClient equivalent of
 * {@link BearerTokenFilterFunction}.
 */
@Order(200)
public class BearerTokenInterceptor implements ClientHttpRequestInterceptor {

    private BearerTokenProvider bearerTokenProvider;

    @Autowired(required = false)
    public void setBearerTokenProvider(BearerTokenProvider bearerTokenProvider) {
        this.bearerTokenProvider = bearerTokenProvider;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                         ClientHttpRequestExecution execution) throws IOException {
        if (bearerTokenProvider == null) {
            return execution.execute(request, body);
        }

        if (request.getHeaders().getFirst(AUTHORIZATION) != null) {
            return execution.execute(request, body);
        }

        String token = bearerTokenProvider.getToken();
        if (token == null || token.isBlank()) {
            return execution.execute(request, body);
        }

        request.getHeaders().set(AUTHORIZATION, "Bearer " + token);
        return execution.execute(request, body);
    }
}
