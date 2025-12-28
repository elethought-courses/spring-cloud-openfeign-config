package tech.elethoughts.courses.cloud.feign.infrastructure;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;
import java.security.SecureRandom;

public class FeignSslContextFactory {

    private final ResourceLoader resourceLoader = new DefaultResourceLoader();
    private final FeignClientProperties properties;

    public FeignSslContextFactory(FeignClientProperties properties) {
        this.properties = properties;
    }

    public SSLContext createSslContext() throws Exception {
        var keyStore = properties.getKeyStore();
        var trustStore = properties.getTrustStore();

        if (keyStore == null && trustStore == null) {
            return SSLContext.getDefault();
        }

        var ssl = SSLContext.getInstance("TLS");
        ssl.init(loadKeyManagers(keyStore), loadTrustManagers(trustStore), new SecureRandom());
        return ssl;
    }

    public SSLContext createDisabledSslContext() {
        try {
            var sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[]{new DisabledValidationTrustManager()}, new SecureRandom());
            return sslContext;
        } catch (Exception e) {
            throw new IllegalStateException("Error creating disabled SSLContext", e);
        }
    }

    private KeyManager[] loadKeyManagers(String keyStore) throws Exception {
        if (keyStore == null) {
            return null;
        }
        var resource = resourceLoader.getResource(keyStore);
        var pwd = properties.getKeyStorePassword();
        var ks = KeyStore.getInstance("PKCS12");
        try (var is = resource.getInputStream()) {
            ks.load(is, pwd);
        }
        var kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, pwd);
        return kmf.getKeyManagers();
    }

    private TrustManager[] loadTrustManagers(String trustStore) throws Exception {
        if (trustStore == null) {
            return null;
        }
        var resource = resourceLoader.getResource(trustStore);
        var pwd = properties.getTrustStorePassword();
        var ks = KeyStore.getInstance("PKCS12");
        try (var is = resource.getInputStream()) {
            ks.load(is, pwd);
        }
        var tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        return tmf.getTrustManagers();
    }
}
