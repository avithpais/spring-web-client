package com.webclient.lib.retry;

import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.util.function.Predicate;

/**
 * Classifies exceptions as retriable (transient) or non-retriable.
 * <p>
 * Retriable:
 * <ul>
 *   <li>HTTP 5xx server errors</li>
 *   <li>HTTP 429 Too Many Requests (rate limiting)</li>
 *   <li>{@link WebClientRequestException} (connection refused, DNS failure,
 *       connect timeout — anything that prevented a response)</li>
 *   <li>{@link IOException} (network-level I/O failures)</li>
 * </ul>
 * Non-retriable:
 * <ul>
 *   <li>HTTP 4xx client errors (except 429)</li>
 *   <li>All other exceptions (programming errors, serialization failures, etc.)</li>
 * </ul>
 */
public final class RetriableExceptionPredicate implements Predicate<Throwable> {

    public static final RetriableExceptionPredicate INSTANCE = new RetriableExceptionPredicate();

    @Override
    public boolean test(Throwable throwable) {
        if (throwable instanceof WebClientResponseException wcre) {
            int status = wcre.getStatusCode().value();
            return status >= 500 || status == 429;
        }
        if (throwable instanceof WebClientRequestException) {
            return true;
        }
        return throwable instanceof IOException;
    }
}
