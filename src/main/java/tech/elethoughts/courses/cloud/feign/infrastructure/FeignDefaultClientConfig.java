package tech.elethoughts.courses.cloud.feign.infrastructure;

import feign.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.support.FeignHttpClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import java.net.InetSocketAddress;
import java.net.Proxy;

public class FeignDefaultClientConfig {

    private final FeignClientProperties properties;
    private final FeignSslContextFactory sslContextFactory;

    public FeignDefaultClientConfig(Environment env,
                                     @Value("${spring.cloud.openfeign.client.name}") String clientName) {
        this.properties = new FeignClientProperties(env, clientName);
        this.sslContextFactory = new FeignSslContextFactory(properties);
    }

    @Bean
    public Client feignClient(FeignHttpClientProperties httpClientProperties) {
        SSLSocketFactory sslSocketFactory = null;
        HostnameVerifier hostnameVerifier = null;

        if (httpClientProperties.isDisableSslValidation()) {
            sslSocketFactory = sslContextFactory.createDisabledSslContext().getSocketFactory();
            hostnameVerifier = (_, _) -> true;
        } else if (properties.isTlsEnabled()) {
            try {
                sslSocketFactory = sslContextFactory.createSslContext().getSocketFactory();
                if (!properties.isVerifyHostname()) {
                    hostnameVerifier = (_, _) -> true;
                }
            } catch (Exception e) {
                throw new IllegalStateException("TLS config failed for: " + properties.getClientName(), e);
            }
        }

        return new Client.Proxied(sslSocketFactory, hostnameVerifier, createProxy());
    }

    private Proxy createProxy() {
        if (!properties.isProxyEnabled()) {
            return Proxy.NO_PROXY;
        }
        var host = properties.getProxyHost();
        if (host == null || host.isBlank()) {
            return Proxy.NO_PROXY;
        }
        return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, properties.getProxyPort()));
    }
}
