package com.webclient.lib.ssl;

import com.webclient.lib.config.HttpClientProperties;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.IdentityCipherSuiteFilter;
import io.netty.handler.ssl.JdkSslContext;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedKeyManager;
import java.io.FileInputStream;
import java.security.KeyStore;

/**
 * Initializes SSL context for the HTTP client connection factory.
 * All keystore, truststore, and TLS configuration is encapsulated here.
 */
public class SslConnectionFactoryInitializer {

    private HttpClientProperties properties;

    @Autowired
    public void setProperties(HttpClientProperties properties) {
        this.properties = properties;
    }

    /**
     * Creates a Netty {@link SslContext} based on the configured SSL properties.
     *
     * @return configured SslContext, or {@code null} if SSL is not enabled
     */
    public SslContext createSslContext() {
        HttpClientProperties.Ssl ssl = properties.getSsl();

        if (!ssl.isEnabled()) {
            return null;
        }

        try {
            if (ssl.isBypassVerification()) {
                return SslContextBuilder.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .protocols(ssl.getTlsProtocol())
                        .build();
            }

            KeyManagerFactory kmf = buildKeyManagerFactory(ssl);
            TrustManagerFactory tmf = buildTrustManagerFactory(ssl);

            KeyManager[] keyManagers = resolveKeyManagers(kmf, ssl);
            TrustManager[] trustManagers = tmf != null ? tmf.getTrustManagers() : null;

            SSLContext jdkSslContext = SSLContext.getInstance(ssl.getTlsProtocol());
            jdkSslContext.init(keyManagers, trustManagers, null);

            return new JdkSslContext(
                    jdkSslContext,
                    true,
                    null,
                    IdentityCipherSuiteFilter.INSTANCE,
                    null,
                    ClientAuth.NONE,
                    new String[]{ssl.getTlsProtocol()},
                    false
            );
        } catch (Exception e) {
            throw new SslInitializationException("Failed to create SSL context", e);
        }
    }

    private KeyManager[] resolveKeyManagers(KeyManagerFactory kmf, HttpClientProperties.Ssl ssl) {
        if (kmf == null) {
            return null;
        }

        if (ssl.getKeyAliasName() != null && !ssl.getKeyAliasName().isBlank()) {
            X509ExtendedKeyManager defaultKeyManager = findX509KeyManager(kmf);
            return new KeyManager[]{
                    new AliasSelectingX509KeyManager(defaultKeyManager, ssl.getKeyAliasName())
            };
        }

        return kmf.getKeyManagers();
    }

    private X509ExtendedKeyManager findX509KeyManager(KeyManagerFactory kmf) {
        for (KeyManager km : kmf.getKeyManagers()) {
            if (km instanceof X509ExtendedKeyManager) {
                return (X509ExtendedKeyManager) km;
            }
        }
        throw new SslInitializationException(
                "No X509ExtendedKeyManager found in KeyManagerFactory");
    }

    private KeyManagerFactory buildKeyManagerFactory(HttpClientProperties.Ssl ssl) throws Exception {
        if (ssl.getKeystorePath() == null || ssl.getKeystorePath().isBlank()) {
            return null;
        }

        KeyStore keyStore = KeyStore.getInstance(ssl.getKeystoreType());
        char[] password = ssl.getKeystorePassword() != null
                ? ssl.getKeystorePassword().toCharArray() : null;

        try (FileInputStream fis = new FileInputStream(ssl.getKeystorePath())) {
            keyStore.load(fis, password);
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, password);
        return kmf;
    }

    private TrustManagerFactory buildTrustManagerFactory(HttpClientProperties.Ssl ssl) throws Exception {
        if (ssl.getTruststorePath() == null || ssl.getTruststorePath().isBlank()) {
            return null;
        }

        KeyStore trustStore = KeyStore.getInstance(ssl.getTruststoreType());
        char[] password = ssl.getTruststorePassword() != null
                ? ssl.getTruststorePassword().toCharArray() : null;

        try (FileInputStream fis = new FileInputStream(ssl.getTruststorePath())) {
            trustStore.load(fis, password);
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        return tmf;
    }

    public static class SslInitializationException extends RuntimeException {

        public SslInitializationException(String message) {
            super(message);
        }

        public SslInitializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
