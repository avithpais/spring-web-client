package com.webclient.lib.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "webclient.http")
public class HttpClientProperties {

    private Ssl ssl = new Ssl();
    private Pool pool = new Pool();
    private Timeout timeout = new Timeout();
    private Retry retry = new Retry();

    public Ssl getSsl() {
        return ssl;
    }

    public void setSsl(Ssl ssl) {
        this.ssl = ssl;
    }

    public Pool getPool() {
        return pool;
    }

    public void setPool(Pool pool) {
        this.pool = pool;
    }

    public Timeout getTimeout() {
        return timeout;
    }

    public void setTimeout(Timeout timeout) {
        this.timeout = timeout;
    }

    public Retry getRetry() {
        return retry;
    }

    public void setRetry(Retry retry) {
        this.retry = retry;
    }

    public static class Ssl {

        private boolean enabled = false;
        private String keystorePath;
        private String keystorePassword;
        private String keystoreType = "PKCS12";
        private String truststorePath;
        private String truststorePassword;
        private String truststoreType = "PKCS12";
        private String keyAliasName;
        private boolean bypassVerification = false;
        private String tlsProtocol = "TLSv1.2";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getKeystorePath() { return keystorePath; }
        public void setKeystorePath(String keystorePath) { this.keystorePath = keystorePath; }
        public String getKeystorePassword() { return keystorePassword; }
        public void setKeystorePassword(String keystorePassword) { this.keystorePassword = keystorePassword; }
        public String getKeystoreType() { return keystoreType; }
        public void setKeystoreType(String keystoreType) { this.keystoreType = keystoreType; }
        public String getTruststorePath() { return truststorePath; }
        public void setTruststorePath(String truststorePath) { this.truststorePath = truststorePath; }
        public String getTruststorePassword() { return truststorePassword; }
        public void setTruststorePassword(String truststorePassword) { this.truststorePassword = truststorePassword; }
        public String getTruststoreType() { return truststoreType; }
        public void setTruststoreType(String truststoreType) { this.truststoreType = truststoreType; }
        public String getKeyAliasName() { return keyAliasName; }
        public void setKeyAliasName(String keyAliasName) { this.keyAliasName = keyAliasName; }
        public boolean isBypassVerification() { return bypassVerification; }
        public void setBypassVerification(boolean bypassVerification) { this.bypassVerification = bypassVerification; }
        public String getTlsProtocol() { return tlsProtocol; }
        public void setTlsProtocol(String tlsProtocol) { this.tlsProtocol = tlsProtocol; }
    }

    public static class Pool {

        private int maxConnections = 500;
        private int maxConnectionsPerRoute = 50;

        public int getMaxConnections() { return maxConnections; }
        public void setMaxConnections(int maxConnections) { this.maxConnections = maxConnections; }
        public int getMaxConnectionsPerRoute() { return maxConnectionsPerRoute; }
        public void setMaxConnectionsPerRoute(int maxConnectionsPerRoute) { this.maxConnectionsPerRoute = maxConnectionsPerRoute; }
    }

    public static class Timeout {

        private int connectRequestTimeoutMs = 5000;
        private int timeoutMs = 30000;

        public int getConnectRequestTimeoutMs() { return connectRequestTimeoutMs; }
        public void setConnectRequestTimeoutMs(int connectRequestTimeoutMs) { this.connectRequestTimeoutMs = connectRequestTimeoutMs; }
        public int getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    }

    public static class Retry {

        private int maxRetries = 3;
        private long retryIntervalMs = 1000;

        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
        public long getRetryIntervalMs() { return retryIntervalMs; }
        public void setRetryIntervalMs(long retryIntervalMs) { this.retryIntervalMs = retryIntervalMs; }
    }
}
