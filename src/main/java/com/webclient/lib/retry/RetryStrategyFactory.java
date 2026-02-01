package com.webclient.lib.retry;

import com.webclient.lib.config.HttpClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * Factory that creates Reactor {@link Retry} specifications with exponential
 * backoff, retriable-exception filtering, and per-attempt logging.
 * <p>
 * Only transient errors (5xx, 429, network I/O failures) trigger retries.
 * Client errors (4xx except 429) and other non-transient exceptions propagate
 * immediately without retry.  Each retry attempt is logged at WARN level with
 * the attempt number and failure reason.
 */
public class RetryStrategyFactory {

    private static final Logger log = LoggerFactory.getLogger(RetryStrategyFactory.class);

    private HttpClientProperties properties;

    @Autowired
    public void setProperties(HttpClientProperties properties) {
        this.properties = properties;
    }

    /**
     * Creates a retry spec using global configuration properties.
     */
    public Retry createRetrySpec() {
        return buildRetrySpec(
                properties.getRetry().getMaxRetries(),
                properties.getRetry().getRetryIntervalMs());
    }

    /**
     * Creates a retry spec with explicit per-request parameters.
     */
    public Retry createRetrySpec(int maxRetries, long retryIntervalMs) {
        return buildRetrySpec(maxRetries, retryIntervalMs);
    }

    private Retry buildRetrySpec(int maxRetries, long retryIntervalMs) {
        return Retry.backoff(maxRetries, Duration.ofMillis(retryIntervalMs))
                .filter(RetriableExceptionPredicate.INSTANCE)
                .doBeforeRetry(signal -> log.warn(
                        "Retry attempt {}/{} — {}: {}",
                        signal.totalRetries() + 1,
                        maxRetries,
                        signal.failure().getClass().getSimpleName(),
                        signal.failure().getMessage()));
    }
}
