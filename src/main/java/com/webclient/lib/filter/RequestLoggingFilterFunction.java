package com.webclient.lib.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

/**
 * {@link ExchangeFilterFunction} that logs outgoing requests and their
 * responses at {@code DEBUG} level.
 * <p>
 * Placed last in the filter chain ({@code @Order(300)}) so that it captures
 * the final request after all other filters (correlation ID, bearer token)
 * have added their headers.
 * <p>
 * Enable via logging configuration:
 * <pre>
 * logging.level.com.webclient.lib.filter.RequestLoggingFilterFunction=DEBUG
 * </pre>
 */
@Order(300)
public class RequestLoggingFilterFunction implements ExchangeFilterFunction {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilterFunction.class);

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        if (!log.isDebugEnabled()) {
            return next.exchange(request);
        }

        String method = request.method().name();
        String url = request.url().toString();
        long startNanos = System.nanoTime();

        log.debug(">>> {} {}", method, url);

        return next.exchange(request)
                .doOnNext(response -> {
                    long ms = (System.nanoTime() - startNanos) / 1_000_000;
                    log.debug("<<< {} {} — {} ({}ms)",
                            method, url, response.statusCode().value(), ms);
                })
                .doOnError(error -> {
                    long ms = (System.nanoTime() - startNanos) / 1_000_000;
                    log.debug("<<< {} {} — FAILED ({}ms): {}",
                            method, url, ms, error.getMessage());
                });
    }
}
