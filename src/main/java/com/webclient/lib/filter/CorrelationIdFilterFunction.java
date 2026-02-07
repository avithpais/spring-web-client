package com.webclient.lib.filter;

import org.springframework.core.annotation.Order;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static com.webclient.lib.util.HttpHeaders.CORRELATION_ID;

/**
 * {@link ExchangeFilterFunction} that adds an {@code X-Correlation-Id} header
 * to every outgoing request when one is not already present.
 * <p>
 * Correlation IDs enable distributed tracing across service boundaries.
 * If the caller has already set the header (e.g., propagating from an
 * incoming request), this filter preserves it.
 */
@Order(100)
public class CorrelationIdFilterFunction implements ExchangeFilterFunction {

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        if (request.headers().getFirst(CORRELATION_ID) != null) {
            return next.exchange(request);
        }

        ClientRequest tagged = ClientRequest.from(request)
                .header(CORRELATION_ID, UUID.randomUUID().toString())
                .build();
        return next.exchange(tagged);
    }
}
