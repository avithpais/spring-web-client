package com.webclient.lib.config;

import com.webclient.lib.auth.BearerTokenInterceptor;
import com.webclient.lib.client.RestServiceClient;
import com.webclient.lib.interceptor.CorrelationIdInterceptor;
import com.webclient.lib.interceptor.RequestLoggingInterceptor;
import com.webclient.lib.retry.SyncRetryExecutor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

/**
 * Auto-configuration for RestClient support.
 * <p>
 * Registers interceptor beans and the {@link RestServiceClient} implementation.
 * The {@link RestClient} bean itself is created in {@link WebClientAutoConfiguration}
 * to share the same underlying {@code HttpClient} with WebClient.
 */
@AutoConfiguration(after = WebClientAutoConfiguration.class)
@EnableConfigurationProperties(HttpClientProperties.class)
@ConditionalOnClass(RestClient.class)
@ConditionalOnBean(RestClient.class)
public class RestClientAutoConfiguration {

    // ------------------------------------------------------------------ //
    //  Infrastructure beans
    // ------------------------------------------------------------------ //

    @Bean
    @ConditionalOnMissingBean
    public SyncRetryExecutor syncRetryExecutor() {
        return new SyncRetryExecutor();
    }

    // ------------------------------------------------------------------ //
    //  Interceptor beans (injectable, NOT auto-registered)
    //
    //  Applications inject these into their services and attach them to
    //  individual RestServiceRequest instances via the builder's interceptor() method.
    // ------------------------------------------------------------------ //

    @Bean
    @ConditionalOnMissingBean
    public CorrelationIdInterceptor correlationIdInterceptor() {
        return new CorrelationIdInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean
    public BearerTokenInterceptor bearerTokenInterceptor() {
        return new BearerTokenInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean
    public RequestLoggingInterceptor requestLoggingInterceptor() {
        return new RequestLoggingInterceptor();
    }

    // ------------------------------------------------------------------ //
    //  RestServiceClient
    // ------------------------------------------------------------------ //

    @Bean
    @ConditionalOnMissingBean
    public RestServiceClient restServiceClient() {
        return new RestServiceClient();
    }
}
