package com.webclient.lib.retry;

import com.webclient.lib.config.HttpClientProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RetryStrategyFactoryTest {

    private RetryStrategyFactory factory;
    private HttpClientProperties properties;

    @BeforeEach
    void setUp() {
        properties = new HttpClientProperties();
        factory = new RetryStrategyFactory();
        factory.setProperties(properties);
    }

    @Test
    void createRetrySpec_returnsNonNullSpec() {
        Retry retrySpec = factory.createRetrySpec();
        assertNotNull(retrySpec);
    }

    @Test
    void createRetrySpec_retriesConfiguredNumberOfTimes() {
        properties.getRetry().setMaxRetries(3);
        properties.getRetry().setRetryIntervalMs(100);

        Retry retrySpec = factory.createRetrySpec();
        AtomicInteger attempts = new AtomicInteger(0);

        Mono<String> mono = Mono.<String>defer(() -> {
            attempts.incrementAndGet();
            return Mono.error(WebClientResponseException.create(
                    500, "Server Error", null, null, null));
        }).retryWhen(retrySpec);

        StepVerifier.create(mono)
                .expectError()
                .verify(Duration.ofSeconds(30));

        // 1 initial attempt + 3 retries = 4 total
        assertEquals(4, attempts.get());
    }

    @Test
    void createRetrySpec_succeedsOnRetry() {
        properties.getRetry().setMaxRetries(3);
        properties.getRetry().setRetryIntervalMs(100);

        Retry retrySpec = factory.createRetrySpec();
        AtomicInteger attempts = new AtomicInteger(0);

        Mono<String> mono = Mono.<String>defer(() -> {
            if (attempts.incrementAndGet() < 3) {
                return Mono.error(WebClientResponseException.create(
                        500, "Server Error", null, null, null));
            }
            return Mono.just("success");
        }).retryWhen(retrySpec);

        StepVerifier.create(mono)
                .expectNext("success")
                .verifyComplete();

        assertEquals(3, attempts.get());
    }

    @Test
    void createRetrySpec_withExplicitParams_usesProvidedValues() {
        Retry retrySpec = factory.createRetrySpec(2, 50);
        AtomicInteger attempts = new AtomicInteger(0);

        Mono<String> mono = Mono.<String>defer(() -> {
            attempts.incrementAndGet();
            return Mono.error(WebClientResponseException.create(
                    500, "Server Error", null, null, null));
        }).retryWhen(retrySpec);

        StepVerifier.create(mono)
                .expectError()
                .verify(Duration.ofSeconds(30));

        // 1 initial attempt + 2 retries = 3 total
        assertEquals(3, attempts.get());
    }

    @Test
    void createRetrySpec_zeroRetries_noRetry() {
        properties.getRetry().setMaxRetries(0);
        properties.getRetry().setRetryIntervalMs(100);

        Retry retrySpec = factory.createRetrySpec();
        AtomicInteger attempts = new AtomicInteger(0);

        Mono<String> mono = Mono.<String>defer(() -> {
            attempts.incrementAndGet();
            return Mono.error(WebClientResponseException.create(
                    500, "Server Error", null, null, null));
        }).retryWhen(retrySpec);

        StepVerifier.create(mono)
                .expectError()
                .verify(Duration.ofSeconds(5));

        assertEquals(1, attempts.get());
    }

    @Test
    void createRetrySpec_nonRetriableException_doesNotRetry() {
        properties.getRetry().setMaxRetries(3);
        properties.getRetry().setRetryIntervalMs(100);

        Retry retrySpec = factory.createRetrySpec();
        AtomicInteger attempts = new AtomicInteger(0);

        // 400 Bad Request is non-retriable
        Mono<String> mono = Mono.<String>defer(() -> {
            attempts.incrementAndGet();
            return Mono.error(WebClientResponseException.create(
                    400, "Bad Request", null, null, null));
        }).retryWhen(retrySpec);

        StepVerifier.create(mono)
                .expectError(WebClientResponseException.class)
                .verify(Duration.ofSeconds(5));

        assertEquals(1, attempts.get());
    }

    @Test
    void createRetrySpec_429TooManyRequests_isRetriable() {
        Retry retrySpec = factory.createRetrySpec(1, 50);
        AtomicInteger attempts = new AtomicInteger(0);

        Mono<String> mono = Mono.<String>defer(() -> {
            attempts.incrementAndGet();
            return Mono.error(WebClientResponseException.create(
                    429, "Too Many Requests", null, null, null));
        }).retryWhen(retrySpec);

        StepVerifier.create(mono)
                .expectError()
                .verify(Duration.ofSeconds(30));

        // 1 initial + 1 retry = 2 total (429 IS retriable)
        assertEquals(2, attempts.get());
    }
}
