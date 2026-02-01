package com.webclient.lib.client;

import com.webclient.lib.config.HttpClientProperties;
import com.webclient.lib.model.WebServiceRequest;
import com.webclient.lib.retry.RetryStrategyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class WebServiceClientTest {

    private WebClient webClient;
    private RetryStrategyFactory retryStrategyFactory;
    private HttpClientProperties properties;
    private WebServiceClient serviceClient;

    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    private WebClient.RequestBodySpec requestBodySpec;

    @BeforeEach
    void setUp() {
        webClient = mock(WebClient.class);
        retryStrategyFactory = new RetryStrategyFactory();

        properties = new HttpClientProperties();
        properties.getRetry().setMaxRetries(2);
        properties.getRetry().setRetryIntervalMs(100);
        properties.getTimeout().setTimeoutMs(30000);
        retryStrategyFactory.setProperties(properties);

        serviceClient = new WebServiceClient();
        serviceClient.setWebClient(webClient);
        serviceClient.setRetryStrategyFactory(retryStrategyFactory);
        serviceClient.setProperties(properties);

        requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        requestBodySpec = mock(WebClient.RequestBodySpec.class);

        when(webClient.method(any(HttpMethod.class))).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(any(MediaType[].class))).thenReturn(requestBodySpec);
    }

    @Test
    void execute_get_success() {
        WebServiceRequest<String> spec = WebServiceRequest.<String>builder()
                .url("https://example.com/api")
                .method(HttpMethod.GET)
                .responseType(String.class)
                .build();

        mockExchangeToMono(requestBodySpec, 200, "response-body");

        StepVerifier.create(serviceClient.execute(spec))
                .expectNext("response-body")
                .verifyComplete();

        verify(webClient).method(HttpMethod.GET);
        verify(requestBodyUriSpec).uri("https://example.com/api");
    }

    @Test
    void execute_post_withBody() {
        String requestBody = "{\"key\":\"value\"}";

        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        doReturn(headersSpec).when(requestBodySpec).bodyValue(any());

        WebServiceRequest<String> spec = WebServiceRequest.<String>builder()
                .url("https://example.com/api")
                .method(HttpMethod.POST)
                .body(requestBody)
                .contentType(MediaType.APPLICATION_JSON)
                .responseType(String.class)
                .build();

        mockExchangeToMono(headersSpec, 200, "created");

        StepVerifier.create(serviceClient.execute(spec))
                .expectNext("created")
                .verifyComplete();

        verify(webClient).method(HttpMethod.POST);
        verify(requestBodySpec).contentType(MediaType.APPLICATION_JSON);
        verify(requestBodySpec).bodyValue(requestBody);
    }

    @Test
    void execute_withHeaders_setsHeaders() {
        WebServiceRequest<String> spec = WebServiceRequest.<String>builder()
                .url("https://example.com/api")
                .header("X-Custom", "value")
                .acceptType(MediaType.APPLICATION_XML)
                .responseType(String.class)
                .build();

        mockExchangeToMono(requestBodySpec, 200, "ok");

        StepVerifier.create(serviceClient.execute(spec))
                .expectNext("ok")
                .verifyComplete();

        verify(requestBodySpec).headers(any());
        verify(requestBodySpec).accept(MediaType.APPLICATION_XML);
    }

    @Test
    void execute_serverError_retriesAndEventuallyFails() {
        WebServiceRequest<String> spec = WebServiceRequest.<String>builder()
                .url("https://example.com/api")
                .responseType(String.class)
                .build();

        mockExchangeToMono(requestBodySpec, 500, "error");

        StepVerifier.create(serviceClient.execute(spec))
                .expectError()
                .verify(Duration.ofSeconds(30));
    }

    @Test
    void execute_serverErrorThenSuccess_retriesAndSucceeds() {
        WebServiceRequest<String> spec = WebServiceRequest.<String>builder()
                .url("https://example.com/api")
                .responseType(String.class)
                .build();

        AtomicInteger callCount = new AtomicInteger(0);

        when(requestBodySpec.exchangeToMono(any(Function.class))).thenAnswer(invocation -> {
            Function<ClientResponse, Mono<String>> handler = invocation.getArgument(0);
            return Mono.defer(() -> {
                ClientResponse response = mock(ClientResponse.class);
                if (callCount.incrementAndGet() < 2) {
                    when(response.statusCode()).thenReturn(HttpStatusCode.valueOf(500));
                    when(response.createException()).thenReturn(Mono.just(
                            WebClientResponseException.create(500, "Error",
                                    null, null, null)));
                    return handler.apply(response);
                }
                when(response.statusCode()).thenReturn(HttpStatusCode.valueOf(200));
                when(response.bodyToMono(String.class)).thenReturn(Mono.just("success"));
                return handler.apply(response);
            });
        });

        StepVerifier.create(serviceClient.execute(spec))
                .expectNext("success")
                .verifyComplete();
    }

    @Test
    void execute_clientError_doesNotRetry() {
        WebServiceRequest<String> spec = WebServiceRequest.<String>builder()
                .url("https://example.com/api")
                .responseType(String.class)
                .build();

        AtomicInteger callCount = new AtomicInteger(0);

        when(requestBodySpec.exchangeToMono(any(Function.class))).thenAnswer(invocation -> {
            Function<ClientResponse, Mono<String>> handler = invocation.getArgument(0);
            return Mono.defer(() -> {
                callCount.incrementAndGet();
                ClientResponse response = mock(ClientResponse.class);
                when(response.statusCode()).thenReturn(HttpStatusCode.valueOf(400));
                when(response.createException()).thenReturn(Mono.just(
                        WebClientResponseException.create(400, "Bad Request",
                                null, null, null)));
                return handler.apply(response);
            });
        });

        StepVerifier.create(serviceClient.execute(spec))
                .expectError(WebClientResponseException.class)
                .verify(Duration.ofSeconds(5));

        assertEquals(1, callCount.get());
    }

    @Test
    void execute_putMethod_usesCorrectVerb() {
        WebServiceRequest<String> spec = WebServiceRequest.<String>builder()
                .url("https://example.com/resource/1")
                .method(HttpMethod.PUT)
                .body("{\"updated\":true}")
                .contentType(MediaType.APPLICATION_JSON)
                .responseType(String.class)
                .build();

        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        doReturn(headersSpec).when(requestBodySpec).bodyValue(any());
        mockExchangeToMono(headersSpec, 200, "updated");

        StepVerifier.create(serviceClient.execute(spec))
                .expectNext("updated")
                .verifyComplete();

        verify(webClient).method(HttpMethod.PUT);
    }

    @Test
    void execute_deleteMethod_usesCorrectVerb() {
        WebServiceRequest<String> spec = WebServiceRequest.<String>builder()
                .url("https://example.com/resource/1")
                .method(HttpMethod.DELETE)
                .responseType(String.class)
                .build();

        mockExchangeToMono(requestBodySpec, 200, "deleted");

        StepVerifier.create(serviceClient.execute(spec))
                .expectNext("deleted")
                .verifyComplete();

        verify(webClient).method(HttpMethod.DELETE);
    }

    @Test
    void execute_withPerRequestTimeout_appliesTimeout() {
        WebServiceRequest<String> spec = WebServiceRequest.<String>builder()
                .url("https://example.com/api")
                .responseType(String.class)
                .timeoutMs(50)
                .build();

        when(requestBodySpec.exchangeToMono(any(Function.class)))
                .thenReturn(Mono.never());

        StepVerifier.create(serviceClient.execute(spec))
                .expectError(TimeoutException.class)
                .verify(Duration.ofSeconds(5));
    }

    @Test
    void execute_withPerRequestMaxRetriesZero_doesNotRetry() {
        WebServiceRequest<String> spec = WebServiceRequest.<String>builder()
                .url("https://example.com/api")
                .responseType(String.class)
                .maxRetries(0)
                .build();

        AtomicInteger callCount = new AtomicInteger(0);
        when(requestBodySpec.exchangeToMono(any(Function.class))).thenAnswer(invocation -> {
            Function<ClientResponse, Mono<String>> handler = invocation.getArgument(0);
            return Mono.defer(() -> {
                callCount.incrementAndGet();
                ClientResponse response = mock(ClientResponse.class);
                when(response.statusCode()).thenReturn(HttpStatusCode.valueOf(500));
                when(response.createException()).thenReturn(Mono.just(
                        WebClientResponseException.create(500, "Error",
                                null, null, null)));
                return handler.apply(response);
            });
        });

        StepVerifier.create(serviceClient.execute(spec))
                .expectError()
                .verify(Duration.ofSeconds(5));

        assertEquals(1, callCount.get());
    }

    @Test
    void execute_withPerRequestRetryOverride_usesSpecValues() {
        WebServiceRequest<String> spec = WebServiceRequest.<String>builder()
                .url("https://example.com/api")
                .responseType(String.class)
                .maxRetries(1)
                .retryIntervalMs(50L)
                .build();

        AtomicInteger callCount = new AtomicInteger(0);
        when(requestBodySpec.exchangeToMono(any(Function.class))).thenAnswer(invocation -> {
            Function<ClientResponse, Mono<String>> handler = invocation.getArgument(0);
            return Mono.defer(() -> {
                callCount.incrementAndGet();
                ClientResponse response = mock(ClientResponse.class);
                when(response.statusCode()).thenReturn(HttpStatusCode.valueOf(500));
                when(response.createException()).thenReturn(Mono.just(
                        WebClientResponseException.create(500, "Error",
                                null, null, null)));
                return handler.apply(response);
            });
        });

        StepVerifier.create(serviceClient.execute(spec))
                .expectError()
                .verify(Duration.ofSeconds(30));

        // 1 initial + 1 retry = 2 total (per-request maxRetries=1, not global 2)
        assertEquals(2, callCount.get());
    }

    @Test
    void execute_withoutOverrides_usesGlobalDefaults() {
        WebServiceRequest<String> spec = WebServiceRequest.<String>builder()
                .url("https://example.com/api")
                .responseType(String.class)
                .build();

        AtomicInteger callCount = new AtomicInteger(0);
        when(requestBodySpec.exchangeToMono(any(Function.class))).thenAnswer(invocation -> {
            Function<ClientResponse, Mono<String>> handler = invocation.getArgument(0);
            return Mono.defer(() -> {
                callCount.incrementAndGet();
                ClientResponse response = mock(ClientResponse.class);
                when(response.statusCode()).thenReturn(HttpStatusCode.valueOf(500));
                when(response.createException()).thenReturn(Mono.just(
                        WebClientResponseException.create(500, "Error",
                                null, null, null)));
                return handler.apply(response);
            });
        });

        StepVerifier.create(serviceClient.execute(spec))
                .expectError()
                .verify(Duration.ofSeconds(30));

        // 1 initial + 2 retries (global maxRetries=2) = 3 total
        assertEquals(3, callCount.get());
    }

    @Test
    void execute_withFilter_appliesFilterViaWebClientMutate() {
        // Arrange: a filter that adds a custom header
        ExchangeFilterFunction testFilter = (request, next) -> {
            ClientRequest modified = ClientRequest.from(request)
                    .header("X-Test-Filter", "applied")
                    .build();
            return next.exchange(modified);
        };

        // Set up the mutate() chain on the mock WebClient
        WebClient.Builder mutatedBuilder = mock(WebClient.Builder.class);
        WebClient mutatedClient = mock(WebClient.class);

        when(webClient.mutate()).thenReturn(mutatedBuilder);
        when(mutatedBuilder.filter(any(ExchangeFilterFunction.class))).thenReturn(mutatedBuilder);
        when(mutatedBuilder.build()).thenReturn(mutatedClient);

        // Wire the mutated client to return the same mock chain
        WebClient.RequestBodyUriSpec mutatedUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec mutatedBodySpec = mock(WebClient.RequestBodySpec.class);
        when(mutatedClient.method(any(HttpMethod.class))).thenReturn(mutatedUriSpec);
        when(mutatedUriSpec.uri(anyString())).thenReturn(mutatedBodySpec);
        when(mutatedBodySpec.headers(any())).thenReturn(mutatedBodySpec);
        when(mutatedBodySpec.accept(any(MediaType[].class))).thenReturn(mutatedBodySpec);
        mockExchangeToMono(mutatedBodySpec, 200, "filtered-response");

        WebServiceRequest<String> spec = WebServiceRequest.<String>builder()
                .url("https://example.com/api")
                .responseType(String.class)
                .filter(testFilter)
                .build();

        StepVerifier.create(serviceClient.execute(spec))
                .expectNext("filtered-response")
                .verifyComplete();

        // Verify that mutate() was called and the filter was applied
        verify(webClient).mutate();
        verify(mutatedBuilder).filter(testFilter);
        verify(mutatedBuilder).build();
    }

    @Test
    void execute_withNoFilters_usesBaseWebClientDirectly() {
        WebServiceRequest<String> spec = WebServiceRequest.<String>builder()
                .url("https://example.com/api")
                .responseType(String.class)
                .build();

        mockExchangeToMono(requestBodySpec, 200, "direct-response");

        StepVerifier.create(serviceClient.execute(spec))
                .expectNext("direct-response")
                .verifyComplete();

        // The base webClient.method() should be called directly (no mutate())
        verify(webClient).method(HttpMethod.GET);
    }

    private void mockExchangeToMono(WebClient.RequestHeadersSpec<?> spec,
                                     int statusCode, String body) {
        when(spec.exchangeToMono(any(Function.class))).thenAnswer(invocation -> {
            Function<ClientResponse, Mono<String>> handler = invocation.getArgument(0);
            ClientResponse response = mock(ClientResponse.class);
            when(response.statusCode()).thenReturn(HttpStatusCode.valueOf(statusCode));

            if (statusCode == 200) {
                when(response.bodyToMono(String.class)).thenReturn(Mono.just(body));
            } else {
                when(response.createException()).thenReturn(Mono.just(
                        WebClientResponseException.create(statusCode, "Error",
                                null, null, null)));
            }

            return handler.apply(response);
        });
    }
}
