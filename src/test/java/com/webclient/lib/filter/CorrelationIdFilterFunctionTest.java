package com.webclient.lib.filter;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CorrelationIdFilterFunctionTest {

    private CorrelationIdFilterFunction filter;
    private ExchangeFunction exchangeFunction;
    private ClientResponse clientResponse;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilterFunction();
        exchangeFunction = mock(ExchangeFunction.class);
        clientResponse = mock(ClientResponse.class);
        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(clientResponse));
    }

    @Test
    void filter_addsCorrelationIdWhenAbsent() {
        ClientRequest request = ClientRequest.create(
                org.springframework.http.HttpMethod.GET,
                URI.create("https://example.com/api"))
                .build();

        StepVerifier.create(filter.filter(request, exchangeFunction))
                .expectNext(clientResponse)
                .verifyComplete();

        var captor = org.mockito.ArgumentCaptor.forClass(ClientRequest.class);
        verify(exchangeFunction).exchange(captor.capture());
        String correlationId = captor.getValue().headers()
                .getFirst(CorrelationIdFilterFunction.CORRELATION_ID_HEADER);
        assertNotNull(correlationId);
    }

    @Test
    void filter_preservesExistingCorrelationId() {
        ClientRequest request = ClientRequest.create(
                org.springframework.http.HttpMethod.GET,
                URI.create("https://example.com/api"))
                .header(CorrelationIdFilterFunction.CORRELATION_ID_HEADER, "existing-id")
                .build();

        StepVerifier.create(filter.filter(request, exchangeFunction))
                .expectNext(clientResponse)
                .verifyComplete();

        var captor = org.mockito.ArgumentCaptor.forClass(ClientRequest.class);
        verify(exchangeFunction).exchange(captor.capture());
        assertEquals("existing-id", captor.getValue().headers()
                .getFirst(CorrelationIdFilterFunction.CORRELATION_ID_HEADER));
    }
}
