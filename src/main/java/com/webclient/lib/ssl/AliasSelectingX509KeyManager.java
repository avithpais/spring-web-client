package com.webclient.lib.ssl;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;
import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * A custom {@link X509ExtendedKeyManager} that always selects a specific key alias
 * from the keystore, overriding the default JDK behavior which assumes one key per keystore.
 * <p>
 * This decorator wraps an existing {@link X509ExtendedKeyManager} and returns the configured
 * alias for all alias-selection methods, while delegating certificate and key retrieval
 * to the wrapped manager.
 */
public class AliasSelectingX509KeyManager extends X509ExtendedKeyManager {

    private final X509ExtendedKeyManager delegate;
    private final String alias;

    public AliasSelectingX509KeyManager(X509ExtendedKeyManager delegate, String alias) {
        this.delegate = delegate;
        this.alias = alias;
    }

    @Override
    public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
        return alias;
    }

    @Override
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        return alias;
    }

    @Override
    public String chooseEngineClientAlias(String[] keyType, Principal[] issuers, SSLEngine engine) {
        return alias;
    }

    @Override
    public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
        return alias;
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        return delegate.getCertificateChain(alias);
    }

    @Override
    public PrivateKey getPrivateKey(String alias) {
        return delegate.getPrivateKey(alias);
    }

    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        return delegate.getClientAliases(keyType, issuers);
    }

    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        return delegate.getServerAliases(keyType, issuers);
    }
}
