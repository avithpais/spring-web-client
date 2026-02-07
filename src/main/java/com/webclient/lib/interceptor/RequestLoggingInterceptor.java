package com.webclient.lib.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * {@link ClientHttpRequestInterceptor} that logs outgoing requests and their
 * responses at {@code DEBUG} level.
 * <p>
 * Placed last in the interceptor chain ({@code @Order(300)}) so that it captures
 * the final request after all other interceptors (correlation ID, bearer token)
 * have added their headers.
 * <p>
 * Enable via logging configuration:
 * <pre>
 * logging.level.com.webclient.lib.interceptor.RequestLoggingInterceptor=DEBUG
 * </pre>
 * <p>
 * This is the synchronous RestClient equivalent of
 * {@link com.webclient.lib.filter.RequestLoggingFilterFunction}.
 */
@Order(300)
public class RequestLoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                         ClientHttpRequestExecution execution) throws IOException {
        if (!log.isDebugEnabled()) {
            return execution.execute(request, body);
        }

        String method = request.getMethod().name();
        String url = request.getURI().toString();
        long startNanos = System.nanoTime();

        log.debug(">>> {} {}", method, url);

        try {
            ClientHttpResponse response = execution.execute(request, body);
            long ms = (System.nanoTime() - startNanos) / 1_000_000;
            log.debug("<<< {} {} - {} ({}ms)", method, url, response.getStatusCode().value(), ms);
            return response;
        } catch (IOException e) {
            long ms = (System.nanoTime() - startNanos) / 1_000_000;
            log.debug("<<< {} {} - FAILED ({}ms): {}", method, url, ms, e.getMessage());
            throw e;
        }
    }
}
