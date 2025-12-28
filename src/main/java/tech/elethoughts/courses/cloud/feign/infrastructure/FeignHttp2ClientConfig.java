package tech.elethoughts.courses.cloud.feign.infrastructure;

import feign.Client;
import feign.http2client.Http2Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.clientconfig.http2client.Http2ClientCustomizer;
import org.springframework.cloud.openfeign.support.FeignHttpClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

public class FeignHttp2ClientConfig {

    private final FeignClientProperties properties;
    private final FeignSslContextFactory sslContextFactory;

    public FeignHttp2ClientConfig(Environment env,
                                   @Value("${spring.cloud.openfeign.client.name}") String clientName) {
        this.properties = new FeignClientProperties(env, clientName);
        this.sslContextFactory = new FeignSslContextFactory(properties);
    }

    @Bean
    public HttpClient.Builder httpClientBuilder(FeignHttpClientProperties httpClientProperties) {
        return HttpClient.newBuilder()
                .followRedirects(httpClientProperties.isFollowRedirects()
                        ? HttpClient.Redirect.ALWAYS
                        : HttpClient.Redirect.NEVER)
                .version(HttpClient.Version.valueOf(httpClientProperties.getHttp2().getVersion()))
                .connectTimeout(Duration.ofMillis(httpClientProperties.getConnectionTimeout()));
    }

    @Bean
    public HttpClient httpClient(HttpClient.Builder httpClientBuilder, List<Http2ClientCustomizer> customizers) {
        customizers.forEach(customizer -> customizer.customize(httpClientBuilder));
        return httpClientBuilder.build();
    }

    @Bean
    public Client feignClient(HttpClient httpClient) {
        return new Http2Client(httpClient);
    }

    @Bean
    public Http2ClientCustomizer proxyCustomizer() {
        return builder -> {
            if (!properties.isProxyEnabled()) {
                return;
            }
            var host = properties.getProxyHost();
            if (host != null && !host.isBlank()) {
                builder.proxy(ProxySelector.of(new InetSocketAddress(host, properties.getProxyPort())));
            }
        };
    }

    @Bean
    public Http2ClientCustomizer tlsCustomizer() {
        return builder -> {
            if (!properties.isTlsEnabled()) {
                return;
            }
            try {
                builder.sslContext(sslContextFactory.createSslContext());
            } catch (Exception e) {
                throw new IllegalStateException("TLS config failed for: " + properties.getClientName(), e);
            }
        };
    }
}
