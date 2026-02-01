package com.webclient.lib.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RequestLoggingFilterFunctionTest {

    private RequestLoggingFilterFunction filter;
    private ExchangeFunction exchangeFunction;

    @BeforeEach
    void setUp() {
        filter = new RequestLoggingFilterFunction();
        exchangeFunction = mock(ExchangeFunction.class);
    }

    @Test
    void filter_passesRequestThrough_onSuccess() {
        ClientResponse clientResponse = mock(ClientResponse.class);
        when(clientResponse.statusCode()).thenReturn(HttpStatusCode.valueOf(200));
        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(clientResponse));

        ClientRequest request = ClientRequest.create(
                org.springframework.http.HttpMethod.GET,
                URI.create("https://example.com/api"))
                .build();

        StepVerifier.create(filter.filter(request, exchangeFunction))
                .expectNext(clientResponse)
                .verifyComplete();

        verify(exchangeFunction).exchange(any(ClientRequest.class));
    }

    @Test
    void filter_propagatesError() {
        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("connection failed")));

        ClientRequest request = ClientRequest.create(
                org.springframework.http.HttpMethod.POST,
                URI.create("https://example.com/api"))
                .build();

        StepVerifier.create(filter.filter(request, exchangeFunction))
                .expectError(RuntimeException.class)
                .verify();
    }
}
