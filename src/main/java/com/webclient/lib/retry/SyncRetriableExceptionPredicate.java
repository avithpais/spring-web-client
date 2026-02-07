package com.webclient.lib.retry;

import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.util.function.Predicate;

/**
 * Classifies exceptions as retriable (transient) or non-retriable for synchronous
 * RestClient operations.
 * <p>
 * This is the synchronous equivalent of {@link RetriableExceptionPredicate}.
 * <p>
 * Retriable:
 * <ul>
 *   <li>HTTP 5xx server errors</li>
 *   <li>HTTP 429 Too Many Requests (rate limiting)</li>
 *   <li>{@link ResourceAccessException} (connection refused, DNS failure,
 *       connect timeout — anything that prevented a response)</li>
 *   <li>{@link IOException} (network-level I/O failures)</li>
 * </ul>
 * Non-retriable:
 * <ul>
 *   <li>HTTP 4xx client errors (except 429)</li>
 *   <li>All other exceptions (programming errors, serialization failures, etc.)</li>
 * </ul>
 */
public final class SyncRetriableExceptionPredicate implements Predicate<Throwable> {

    public static final SyncRetriableExceptionPredicate INSTANCE = new SyncRetriableExceptionPredicate();

    @Override
    public boolean test(Throwable throwable) {
        if (throwable instanceof HttpStatusCodeException hsce) {
            int status = hsce.getStatusCode().value();
            return status >= 500 || status == 429;
        }
        if (throwable instanceof ResourceAccessException) {
            return true;
        }
        return throwable instanceof IOException;
    }
}
