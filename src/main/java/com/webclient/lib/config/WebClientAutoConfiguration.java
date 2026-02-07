package com.webclient.lib.config;

import com.webclient.lib.auth.BearerTokenFilterFunction;
import com.webclient.lib.client.WebServiceClient;
import com.webclient.lib.filter.CorrelationIdFilterFunction;
import com.webclient.lib.filter.RequestLoggingFilterFunction;
import com.webclient.lib.retry.RetryStrategyFactory;
import com.webclient.lib.ssl.SslConnectionFactoryInitializer;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@AutoConfiguration
@EnableConfigurationProperties(HttpClientProperties.class)
@ConditionalOnClass(WebClient.class)
public class WebClientAutoConfiguration {

    private HttpClientProperties properties;

    @Autowired
    public void setProperties(HttpClientProperties properties) {
        this.properties = properties;
    }

    // ------------------------------------------------------------------ //
    //  Infrastructure beans
    // ------------------------------------------------------------------ //

    @Bean
    @ConditionalOnMissingBean
    public SslConnectionFactoryInitializer sslConnectionFactoryInitializer() {
        return new SslConnectionFactoryInitializer();
    }

    @Bean
    @ConditionalOnMissingBean
    public RetryStrategyFactory retryStrategyFactory() {
        return new RetryStrategyFactory();
    }

    // ------------------------------------------------------------------ //
    //  Exchange filter function beans (injectable, NOT auto-registered)
    //
    //  Applications inject these into their services and attach them to
    //  individual WebServiceRequest instances via the builder's filter() method.
    // ------------------------------------------------------------------ //

    @Bean
    @ConditionalOnMissingBean
    public CorrelationIdFilterFunction correlationIdFilterFunction() {
        return new CorrelationIdFilterFunction();
    }

    @Bean
    @ConditionalOnMissingBean
    public BearerTokenFilterFunction bearerTokenFilterFunction() {
        return new BearerTokenFilterFunction();
    }

    @Bean
    @ConditionalOnMissingBean
    public RequestLoggingFilterFunction requestLoggingFilterFunction() {
        return new RequestLoggingFilterFunction();
    }

    // ------------------------------------------------------------------ //
    //  Connection and HTTP client
    // ------------------------------------------------------------------ //

    @Bean
    @ConditionalOnMissingBean(name = "webClientConnectionProvider")
    public ConnectionProvider webClientConnectionProvider() {
        return ConnectionProvider.builder("webclient-pool")
                .maxConnections(properties.getPool().getMaxConnections())
                .pendingAcquireTimeout(
                        Duration.ofMillis(properties.getTimeout().getConnectRequestTimeoutMs()))
                .maxIdleTime(Duration.ofSeconds(60))
                .evictInBackground(Duration.ofSeconds(120))
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(name = "webClientHttpClient")
    public HttpClient webClientHttpClient(
            ConnectionProvider webClientConnectionProvider,
            SslConnectionFactoryInitializer sslInitializer) {

        HttpClient client = HttpClient.create(webClientConnectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        properties.getTimeout().getConnectRequestTimeoutMs())
                .responseTimeout(
                        Duration.ofMillis(properties.getTimeout().getTimeoutMs()));

        SslContext sslContext = sslInitializer.createSslContext();
        if (sslContext != null) {
            client = client.secure(spec -> spec.sslContext(sslContext));
        }

        return client;
    }

    /**
     * Builds the shared {@link WebClient} <b>without</b> any filters.
     * <p>
     * Filters are applied per-request through {@code WebServiceRequest.builder().filter(...)},
     * which calls {@code webClient.mutate()} internally. This gives callers full
     * control over which filters run for each individual call — for example,
     * bearer-token injection may only be needed for certain downstream services.
     */
    @Bean
    @ConditionalOnMissingBean
    public WebClient webClient(HttpClient webClientHttpClient) {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(webClientHttpClient))
                .build();
    }

    /**
     * Builds the shared {@link RestClient} <b>without</b> any interceptors.
     * <p>
     * Uses the same underlying {@link HttpClient} as WebClient, sharing the connection
     * pool, SSL configuration, and timeouts.
     * <p>
     * Interceptors are applied per-request through {@code RestServiceRequest.builder().interceptor(...)},
     * which calls {@code restClient.mutate()} internally. This gives callers full
     * control over which interceptors run for each individual call.
     */
    @Bean
    @ConditionalOnMissingBean
    public RestClient restClient(HttpClient webClientHttpClient) {
        ReactorClientHttpRequestFactory requestFactory =
                new ReactorClientHttpRequestFactory(webClientHttpClient);
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public WebServiceClient webServiceClient() {
        return new WebServiceClient();
    }
}
