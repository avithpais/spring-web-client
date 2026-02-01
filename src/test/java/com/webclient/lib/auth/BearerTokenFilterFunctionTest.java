package com.webclient.lib.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BearerTokenFilterFunctionTest {

    private BearerTokenFilterFunction filterFunction;
    private ExchangeFunction exchangeFunction;
    private ClientResponse clientResponse;

    @BeforeEach
    void setUp() {
        filterFunction = new BearerTokenFilterFunction();
        exchangeFunction = mock(ExchangeFunction.class);
        clientResponse = mock(ClientResponse.class);
        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(clientResponse));
    }

    @Test
    void filter_withProvider_addsAuthorizationHeader() {
        filterFunction.setBearerTokenProvider(() -> "test-token");

        ClientRequest request = ClientRequest.create(
                org.springframework.http.HttpMethod.GET,
                URI.create("https://example.com/api"))
                .build();

        Mono<ClientResponse> result = filterFunction.filter(request, exchangeFunction);

        StepVerifier.create(result)
                .expectNext(clientResponse)
                .verifyComplete();

        var captor = org.mockito.ArgumentCaptor.forClass(ClientRequest.class);
        verify(exchangeFunction).exchange(captor.capture());
        ClientRequest capturedRequest = captor.getValue();
        assertNotNull(capturedRequest.headers().getFirst("Authorization"));
        assertEquals("Bearer test-token",
                capturedRequest.headers().getFirst("Authorization"));
    }

    @Test
    void filter_withoutProvider_passesRequestUnmodified() {
        ClientRequest request = ClientRequest.create(
                org.springframework.http.HttpMethod.GET,
                URI.create("https://example.com/api"))
                .build();

        Mono<ClientResponse> result = filterFunction.filter(request, exchangeFunction);

        StepVerifier.create(result)
                .expectNext(clientResponse)
                .verifyComplete();

        var captor = org.mockito.ArgumentCaptor.forClass(ClientRequest.class);
        verify(exchangeFunction).exchange(captor.capture());
        ClientRequest capturedRequest = captor.getValue();
        assertNull(capturedRequest.headers().getFirst("Authorization"));
    }

    @Test
    void filter_withNullToken_passesRequestUnmodified() {
        filterFunction.setBearerTokenProvider(() -> null);

        ClientRequest request = ClientRequest.create(
                org.springframework.http.HttpMethod.GET,
                URI.create("https://example.com/api"))
                .build();

        Mono<ClientResponse> result = filterFunction.filter(request, exchangeFunction);

        StepVerifier.create(result)
                .expectNext(clientResponse)
                .verifyComplete();

        var captor = org.mockito.ArgumentCaptor.forClass(ClientRequest.class);
        verify(exchangeFunction).exchange(captor.capture());
        assertNull(captor.getValue().headers().getFirst("Authorization"));
    }

    @Test
    void filter_withBlankToken_passesRequestUnmodified() {
        filterFunction.setBearerTokenProvider(() -> "   ");

        ClientRequest request = ClientRequest.create(
                org.springframework.http.HttpMethod.GET,
                URI.create("https://example.com/api"))
                .build();

        Mono<ClientResponse> result = filterFunction.filter(request, exchangeFunction);

        StepVerifier.create(result)
                .expectNext(clientResponse)
                .verifyComplete();

        var captor = org.mockito.ArgumentCaptor.forClass(ClientRequest.class);
        verify(exchangeFunction).exchange(captor.capture());
        assertNull(captor.getValue().headers().getFirst("Authorization"));
    }

    @Test
    void filter_withExistingAuthorizationHeader_doesNotOverwrite() {
        filterFunction.setBearerTokenProvider(() -> "new-token");

        ClientRequest request = ClientRequest.create(
                org.springframework.http.HttpMethod.GET,
                URI.create("https://example.com/api"))
                .header("Authorization", "Bearer existing-token")
                .build();

        Mono<ClientResponse> result = filterFunction.filter(request, exchangeFunction);

        StepVerifier.create(result)
                .expectNext(clientResponse)
                .verifyComplete();

        var captor = org.mockito.ArgumentCaptor.forClass(ClientRequest.class);
        verify(exchangeFunction).exchange(captor.capture());
        assertEquals("Bearer existing-token",
                captor.getValue().headers().getFirst("Authorization"));
    }
}
