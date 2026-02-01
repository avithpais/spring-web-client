package com.webclient.lib.ssl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AliasSelectingX509KeyManagerTest {

    private static final String ALIAS = "myAlias";

    private X509ExtendedKeyManager delegate;
    private AliasSelectingX509KeyManager keyManager;

    @BeforeEach
    void setUp() {
        delegate = mock(X509ExtendedKeyManager.class);
        keyManager = new AliasSelectingX509KeyManager(delegate, ALIAS);
    }

    @Test
    void chooseClientAlias_returnsConfiguredAlias() {
        String result = keyManager.chooseClientAlias(
                new String[]{"RSA"}, null, null);
        assertEquals(ALIAS, result);
    }

    @Test
    void chooseServerAlias_returnsConfiguredAlias() {
        String result = keyManager.chooseServerAlias("RSA", null, null);
        assertEquals(ALIAS, result);
    }

    @Test
    void chooseEngineClientAlias_returnsConfiguredAlias() {
        SSLEngine engine = mock(SSLEngine.class);
        String result = keyManager.chooseEngineClientAlias(
                new String[]{"RSA"}, null, engine);
        assertEquals(ALIAS, result);
    }

    @Test
    void chooseEngineServerAlias_returnsConfiguredAlias() {
        SSLEngine engine = mock(SSLEngine.class);
        String result = keyManager.chooseEngineServerAlias("RSA", null, engine);
        assertEquals(ALIAS, result);
    }

    @Test
    void getCertificateChain_delegatesToWrappedManager() {
        X509Certificate[] certs = new X509Certificate[]{mock(X509Certificate.class)};
        when(delegate.getCertificateChain(ALIAS)).thenReturn(certs);

        X509Certificate[] result = keyManager.getCertificateChain(ALIAS);

        assertArrayEquals(certs, result);
        verify(delegate).getCertificateChain(ALIAS);
    }

    @Test
    void getPrivateKey_delegatesToWrappedManager() {
        PrivateKey key = mock(PrivateKey.class);
        when(delegate.getPrivateKey(ALIAS)).thenReturn(key);

        PrivateKey result = keyManager.getPrivateKey(ALIAS);

        assertEquals(key, result);
        verify(delegate).getPrivateKey(ALIAS);
    }

    @Test
    void getClientAliases_delegatesToWrappedManager() {
        String[] aliases = {"alias1", "alias2"};
        Principal[] issuers = new Principal[]{mock(Principal.class)};
        when(delegate.getClientAliases("RSA", issuers)).thenReturn(aliases);

        String[] result = keyManager.getClientAliases("RSA", issuers);

        assertArrayEquals(aliases, result);
        verify(delegate).getClientAliases("RSA", issuers);
    }

    @Test
    void getServerAliases_delegatesToWrappedManager() {
        String[] aliases = {"alias1", "alias2"};
        Principal[] issuers = new Principal[]{mock(Principal.class)};
        when(delegate.getServerAliases("RSA", issuers)).thenReturn(aliases);

        String[] result = keyManager.getServerAliases("RSA", issuers);

        assertArrayEquals(aliases, result);
        verify(delegate).getServerAliases("RSA", issuers);
    }
}
