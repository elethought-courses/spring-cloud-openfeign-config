package tech.elethoughts.courses.cloud.feign.infrastructure;

import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.clientconfig.HttpClient5FeignConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import javax.net.ssl.SSLContext;

public class FeignHttpClientConfig {

    private static final String PREFIX = "feign.client.config.";

    private final Environment env;
    private final ResourceLoader resources = new DefaultResourceLoader();
    private final String client;

    public FeignHttpClientConfig(Environment env,
                                  @Value("${spring.cloud.openfeign.client.name}") String client) {
        this.env = env;
        this.client = client;
    }

    @Bean
    HttpClient5FeignConfiguration.HttpClientBuilderCustomizer httpClientCustomizer() {
        return builder -> {
            configureConnectionManager(builder);
            configureProxy(builder);
        };
    }

    private void configureConnectionManager(HttpClientBuilder builder) {
        var cmBuilder = PoolingHttpClientConnectionManagerBuilder.create();

        if (bool("tls.enabled", false)) {
            try {
                var sslContext = createSslContext();
                var hostnameVerifier = bool("tls.verify-hostname", true) ? null : NoopHostnameVerifier.INSTANCE;
                cmBuilder.setTlsSocketStrategy(new DefaultClientTlsStrategy(sslContext, hostnameVerifier));
            } catch (Exception e) {
                throw new IllegalStateException("TLS config failed for: " + client, e);
            }
        }
        builder.setConnectionManager(cmBuilder.build());
    }

    private void configureProxy(HttpClientBuilder builder) {
        var host = prop("proxy.host", null);
        if (bool("proxy.enabled", false) && host != null && !host.isBlank()) {
            builder.setRoutePlanner(new DefaultProxyRoutePlanner(
                    new HttpHost(host, integer("proxy.port", 8080))));
        }
    }

    private SSLContext createSslContext() throws Exception {
        var ssl = SSLContextBuilder.create();
        var ks = resource("tls.key-store");
        if (ks != null) {
            var pwd = prop("tls.key-store-password", "").toCharArray();
            ssl.loadKeyMaterial(ks.getURL(), pwd, pwd);
        }
        var ts = resource("tls.trust-store");
        if (ts != null) {
            ssl.loadTrustMaterial(ts.getURL(), prop("tls.trust-store-password", "").toCharArray());
        }
        return ssl.build();
    }

    private String prop(String key, String def) {
        return env.getProperty(PREFIX + client + "." + key, def);
    }

    private boolean bool(String key, boolean def) {
        return Boolean.parseBoolean(prop(key, String.valueOf(def)));
    }

    private int integer(String key, int def) {
        return Integer.parseInt(prop(key, String.valueOf(def)));
    }

    private Resource resource(String key) {
        var loc = prop(key, null);
        return loc != null ? resources.getResource(loc) : null;
    }
}
