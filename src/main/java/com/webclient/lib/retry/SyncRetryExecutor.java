package com.webclient.lib.retry;

import com.webclient.lib.config.HttpClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.function.Supplier;

/**
 * Synchronous retry executor with exponential backoff for RestClient operations.
 * <p>
 * Uses the same exception classification as the reactive {@link RetryStrategyFactory},
 * but implemented for blocking calls. Only transient errors (5xx, 429, network I/O
 * failures) trigger retries. Client errors (4xx except 429) and other non-transient
 * exceptions propagate immediately without retry.
 * <p>
 * Each retry attempt is logged at WARN level with the attempt number and failure reason.
 */
public class SyncRetryExecutor {

    private static final Logger log = LoggerFactory.getLogger(SyncRetryExecutor.class);

    private HttpClientProperties properties;

    @Autowired
    public void setProperties(HttpClientProperties properties) {
        this.properties = properties;
    }

    /**
     * Executes the supplier with retries using global configuration properties.
     *
     * @param operation the operation to execute
     * @param <T>       the return type
     * @return the result of the operation
     */
    public <T> T executeWithRetry(Supplier<T> operation) {
        return executeWithRetry(
                operation,
                properties.getRetry().getMaxRetries(),
                properties.getRetry().getRetryIntervalMs());
    }

    /**
     * Executes the supplier with explicit retry parameters.
     *
     * @param operation       the operation to execute
     * @param maxRetries      maximum number of retry attempts
     * @param retryIntervalMs initial backoff interval in milliseconds
     * @param <T>             the return type
     * @return the result of the operation
     */
    public <T> T executeWithRetry(Supplier<T> operation, int maxRetries, long retryIntervalMs) {
        Throwable lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return operation.get();
            } catch (Throwable e) {
                lastException = e;

                if (!SyncRetriableExceptionPredicate.INSTANCE.test(e)) {
                    throw sneakyThrow(e);
                }

                if (attempt < maxRetries) {
                    log.warn("Retry attempt {}/{} - {}: {}",
                            attempt + 1, maxRetries,
                            e.getClass().getSimpleName(), e.getMessage());

                    long delay = retryIntervalMs * (1L << attempt);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                }
            }
        }

        throw sneakyThrow(lastException);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> RuntimeException sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }
}
