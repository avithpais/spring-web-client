package com.webclient.lib.ssl;

import com.webclient.lib.config.HttpClientProperties;
import io.netty.handler.ssl.SslContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SslConnectionFactoryInitializerTest {

    private static final String PASSWORD = "changeit";
    private static final String ALIAS = "testkey";

    @TempDir
    static Path tempDir;

    private static String keystorePath;
    private static String truststorePath;

    private SslConnectionFactoryInitializer initializer;
    private HttpClientProperties properties;

    @BeforeAll
    static void createTestStores() throws Exception {
        keystorePath = tempDir.resolve("test-keystore.p12").toString();
        truststorePath = tempDir.resolve("test-truststore.p12").toString();

        // Generate keystore with a self-signed key pair using keytool
        runKeytool(
                "-genkeypair",
                "-alias", ALIAS,
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-storetype", "PKCS12",
                "-keystore", keystorePath,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                "-dname", "CN=Test,OU=Test,O=Test,L=Test,ST=Test,C=US",
                "-validity", "1"
        );

        // Export the certificate from keystore
        String certPath = tempDir.resolve("test-cert.pem").toString();
        runKeytool(
                "-exportcert",
                "-alias", ALIAS,
                "-keystore", keystorePath,
                "-storepass", PASSWORD,
                "-file", certPath,
                "-rfc"
        );

        // Import the certificate into a truststore
        runKeytool(
                "-importcert",
                "-alias", "trusted",
                "-keystore", truststorePath,
                "-storepass", PASSWORD,
                "-file", certPath,
                "-noprompt",
                "-storetype", "PKCS12"
        );
    }

    private static void runKeytool(String... args) throws Exception {
        String keytoolPath = System.getProperty("java.home")
                + java.io.File.separator + "bin"
                + java.io.File.separator + "keytool";
        String[] cmd = new String[args.length + 1];
        cmd[0] = keytoolPath;
        System.arraycopy(args, 0, cmd, 1, args.length);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        process.getInputStream().readAllBytes();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("keytool failed with exit code " + exitCode);
        }
    }

    @BeforeEach
    void setUp() {
        properties = new HttpClientProperties();
        initializer = new SslConnectionFactoryInitializer();
        initializer.setProperties(properties);
    }

    @Test
    void createSslContext_sslDisabled_returnsNull() {
        properties.getSsl().setEnabled(false);

        SslContext result = initializer.createSslContext();

        assertNull(result);
    }

    @Test
    void createSslContext_bypassVerification_returnsInsecureContext() {
        properties.getSsl().setEnabled(true);
        properties.getSsl().setBypassVerification(true);

        SslContext result = initializer.createSslContext();

        assertNotNull(result);
        assertTrue(result.isClient());
    }

    @Test
    void createSslContext_withKeystoreAndTruststore_returnsSslContext() {
        properties.getSsl().setEnabled(true);
        properties.getSsl().setKeystorePath(keystorePath);
        properties.getSsl().setKeystorePassword(PASSWORD);
        properties.getSsl().setKeystoreType("PKCS12");
        properties.getSsl().setTruststorePath(truststorePath);
        properties.getSsl().setTruststorePassword(PASSWORD);
        properties.getSsl().setTruststoreType("PKCS12");

        SslContext result = initializer.createSslContext();

        assertNotNull(result);
        assertTrue(result.isClient());
    }

    @Test
    void createSslContext_withKeyAliasName_usesAliasSelectingKeyManager() {
        properties.getSsl().setEnabled(true);
        properties.getSsl().setKeystorePath(keystorePath);
        properties.getSsl().setKeystorePassword(PASSWORD);
        properties.getSsl().setKeystoreType("PKCS12");
        properties.getSsl().setTruststorePath(truststorePath);
        properties.getSsl().setTruststorePassword(PASSWORD);
        properties.getSsl().setTruststoreType("PKCS12");
        properties.getSsl().setKeyAliasName(ALIAS);

        SslContext result = initializer.createSslContext();

        assertNotNull(result);
        assertTrue(result.isClient());
    }

    @Test
    void createSslContext_withTruststoreOnly_returnsSslContext() {
        properties.getSsl().setEnabled(true);
        properties.getSsl().setTruststorePath(truststorePath);
        properties.getSsl().setTruststorePassword(PASSWORD);
        properties.getSsl().setTruststoreType("PKCS12");

        SslContext result = initializer.createSslContext();

        assertNotNull(result);
    }

    @Test
    void createSslContext_invalidKeystorePath_throwsException() {
        properties.getSsl().setEnabled(true);
        properties.getSsl().setKeystorePath("/nonexistent/path.p12");
        properties.getSsl().setKeystorePassword(PASSWORD);

        assertThrows(SslConnectionFactoryInitializer.SslInitializationException.class,
                () -> initializer.createSslContext());
    }

    @Test
    void createSslContext_noKeystoreNoTruststore_returnsSslContext() {
        properties.getSsl().setEnabled(true);

        SslContext result = initializer.createSslContext();

        assertNotNull(result);
    }
}
