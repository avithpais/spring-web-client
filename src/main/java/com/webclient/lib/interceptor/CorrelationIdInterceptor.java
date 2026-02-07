package com.webclient.lib.interceptor;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.UUID;

import static com.webclient.lib.util.HttpHeaders.CORRELATION_ID;

/**
 * {@link ClientHttpRequestInterceptor} that adds an {@code X-Correlation-Id} header
 * to every outgoing request when one is not already present.
 * <p>
 * Correlation IDs enable distributed tracing across service boundaries.
 * If the caller has already set the header (e.g., propagating from an
 * incoming request), this interceptor preserves it.
 * <p>
 * This is the synchronous RestClient equivalent of
 * {@link com.webclient.lib.filter.CorrelationIdFilterFunction}.
 */
@Order(100)
public class CorrelationIdInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                         ClientHttpRequestExecution execution) throws IOException {
        if (request.getHeaders().getFirst(CORRELATION_ID) == null) {
            request.getHeaders().set(CORRELATION_ID, UUID.randomUUID().toString());
        }
        return execution.execute(request, body);
    }
}
