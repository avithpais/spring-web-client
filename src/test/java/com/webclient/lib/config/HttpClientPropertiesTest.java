package com.webclient.lib.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class HttpClientPropertiesTest {

    @Test
    void defaults_sslProperties() {
        HttpClientProperties props = new HttpClientProperties();
        HttpClientProperties.Ssl ssl = props.getSsl();

        assertNotNull(ssl);
        assertFalse(ssl.isEnabled());
        assertNull(ssl.getKeystorePath());
        assertNull(ssl.getKeystorePassword());
        assertEquals("PKCS12", ssl.getKeystoreType());
        assertNull(ssl.getTruststorePath());
        assertNull(ssl.getTruststorePassword());
        assertEquals("PKCS12", ssl.getTruststoreType());
        assertNull(ssl.getKeyAliasName());
        assertFalse(ssl.isBypassVerification());
        assertEquals("TLSv1.2", ssl.getTlsProtocol());
    }

    @Test
    void defaults_poolProperties() {
        HttpClientProperties props = new HttpClientProperties();
        HttpClientProperties.Pool pool = props.getPool();

        assertNotNull(pool);
        assertEquals(500, pool.getMaxConnections());
        assertEquals(50, pool.getMaxConnectionsPerRoute());
    }

    @Test
    void defaults_timeoutProperties() {
        HttpClientProperties props = new HttpClientProperties();
        HttpClientProperties.Timeout timeout = props.getTimeout();

        assertNotNull(timeout);
        assertEquals(5000, timeout.getConnectRequestTimeoutMs());
        assertEquals(30000, timeout.getTimeoutMs());
    }

    @Test
    void defaults_retryProperties() {
        HttpClientProperties props = new HttpClientProperties();
        HttpClientProperties.Retry retry = props.getRetry();

        assertNotNull(retry);
        assertEquals(3, retry.getMaxRetries());
        assertEquals(1000, retry.getRetryIntervalMs());
    }

    @Test
    void settersAndGetters_ssl() {
        HttpClientProperties.Ssl ssl = new HttpClientProperties.Ssl();

        ssl.setEnabled(true);
        ssl.setKeystorePath("/path/to/keystore");
        ssl.setKeystorePassword("kspass");
        ssl.setKeystoreType("JKS");
        ssl.setTruststorePath("/path/to/truststore");
        ssl.setTruststorePassword("tspass");
        ssl.setTruststoreType("JKS");
        ssl.setKeyAliasName("myalias");
        ssl.setBypassVerification(true);
        ssl.setTlsProtocol("TLSv1.3");

        assertEquals(true, ssl.isEnabled());
        assertEquals("/path/to/keystore", ssl.getKeystorePath());
        assertEquals("kspass", ssl.getKeystorePassword());
        assertEquals("JKS", ssl.getKeystoreType());
        assertEquals("/path/to/truststore", ssl.getTruststorePath());
        assertEquals("tspass", ssl.getTruststorePassword());
        assertEquals("JKS", ssl.getTruststoreType());
        assertEquals("myalias", ssl.getKeyAliasName());
        assertEquals(true, ssl.isBypassVerification());
        assertEquals("TLSv1.3", ssl.getTlsProtocol());
    }

    @Test
    void settersAndGetters_pool() {
        HttpClientProperties.Pool pool = new HttpClientProperties.Pool();

        pool.setMaxConnections(100);
        pool.setMaxConnectionsPerRoute(20);

        assertEquals(100, pool.getMaxConnections());
        assertEquals(20, pool.getMaxConnectionsPerRoute());
    }

    @Test
    void settersAndGetters_timeout() {
        HttpClientProperties.Timeout timeout = new HttpClientProperties.Timeout();

        timeout.setConnectRequestTimeoutMs(3000);
        timeout.setTimeoutMs(15000);

        assertEquals(3000, timeout.getConnectRequestTimeoutMs());
        assertEquals(15000, timeout.getTimeoutMs());
    }

    @Test
    void settersAndGetters_retry() {
        HttpClientProperties.Retry retry = new HttpClientProperties.Retry();

        retry.setMaxRetries(5);
        retry.setRetryIntervalMs(2000);

        assertEquals(5, retry.getMaxRetries());
        assertEquals(2000, retry.getRetryIntervalMs());
    }

    @Test
    void settersAndGetters_topLevel() {
        HttpClientProperties props = new HttpClientProperties();

        HttpClientProperties.Ssl ssl = new HttpClientProperties.Ssl();
        ssl.setEnabled(true);
        props.setSsl(ssl);

        HttpClientProperties.Pool pool = new HttpClientProperties.Pool();
        pool.setMaxConnections(200);
        props.setPool(pool);

        HttpClientProperties.Timeout timeout = new HttpClientProperties.Timeout();
        timeout.setTimeoutMs(10000);
        props.setTimeout(timeout);

        HttpClientProperties.Retry retry = new HttpClientProperties.Retry();
        retry.setMaxRetries(2);
        props.setRetry(retry);

        assertEquals(true, props.getSsl().isEnabled());
        assertEquals(200, props.getPool().getMaxConnections());
        assertEquals(10000, props.getTimeout().getTimeoutMs());
        assertEquals(2, props.getRetry().getMaxRetries());
    }
}
