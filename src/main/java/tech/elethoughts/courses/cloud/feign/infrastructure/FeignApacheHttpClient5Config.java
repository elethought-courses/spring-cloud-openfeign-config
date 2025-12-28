package tech.elethoughts.courses.cloud.feign.infrastructure;

import feign.Client;
import feign.hc5.ApacheHttp5Client;
import jakarta.annotation.PreDestroy;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.socket.LayeredConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.clientconfig.HttpClient5FeignConfiguration;
import org.springframework.cloud.openfeign.support.FeignHttpClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FeignApacheHttpClient5Config {

    private final FeignClientProperties properties;
    private final ResourceLoader resourceLoader = new DefaultResourceLoader();
    private CloseableHttpClient httpClient5;

    public FeignApacheHttpClient5Config(Environment env,
                                         @Value("${spring.cloud.openfeign.client.name}") String clientName) {
        this.properties = new FeignClientProperties(env, clientName);
    }

    @Bean
    public Client feignClient(CloseableHttpClient httpClient5) {
        return new ApacheHttp5Client(httpClient5);
    }

    @Bean
    public HttpClientConnectionManager hc5ConnectionManager(FeignHttpClientProperties httpClientProperties) {
        var builder = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(httpsSSLConnectionSocketFactory(httpClientProperties.isDisableSslValidation()))
                .setMaxConnTotal(httpClientProperties.getMaxConnections())
                .setMaxConnPerRoute(httpClientProperties.getMaxConnectionsPerRoute())
                .setConnPoolPolicy(PoolReusePolicy.valueOf(httpClientProperties.getHc5().getPoolReusePolicy().name()))
                .setPoolConcurrencyPolicy(PoolConcurrencyPolicy.valueOf(httpClientProperties.getHc5().getPoolConcurrencyPolicy().name()))
                .setConnectionTimeToLive(TimeValue.of(httpClientProperties.getTimeToLive(), httpClientProperties.getTimeToLiveUnit()))
                .setDefaultSocketConfig(SocketConfig.custom()
                        .setSoTimeout(Timeout.of(httpClientProperties.getHc5().getSocketTimeout(),
                                httpClientProperties.getHc5().getSocketTimeoutUnit()))
                        .build());

        configureTls(builder);

        return builder.build();
    }

    @Bean
    public CloseableHttpClient httpClient5(
            HttpClientConnectionManager connectionManager,
            FeignHttpClientProperties httpClientProperties,
            ObjectProvider<List<HttpClient5FeignConfiguration.HttpClientBuilderCustomizer>> customizerProvider) {

        var builder = HttpClients.custom()
                .disableCookieManagement()
                .useSystemProperties()
                .setConnectionManager(connectionManager)
                .evictExpiredConnections()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(Timeout.of(httpClientProperties.getConnectionTimeout(), TimeUnit.MILLISECONDS))
                        .setRedirectsEnabled(httpClientProperties.isFollowRedirects())
                        .setConnectionRequestTimeout(Timeout.of(httpClientProperties.getHc5().getConnectionRequestTimeout(),
                                httpClientProperties.getHc5().getConnectionRequestTimeoutUnit()))
                        .build());

        customizerProvider.getIfAvailable(List::of).forEach(c -> c.customize(builder));

        httpClient5 = builder.build();
        return httpClient5;
    }

    @Bean
    public HttpClient5FeignConfiguration.HttpClientBuilderCustomizer proxyCustomizer() {
        return builder -> {
            if (!properties.isProxyEnabled()) {
                return;
            }
            var host = properties.getProxyHost();
            if (host != null && !host.isBlank()) {
                builder.setRoutePlanner(new DefaultProxyRoutePlanner(new HttpHost(host, properties.getProxyPort())));
            }
        };
    }

    @PreDestroy
    public void destroy() {
        if (httpClient5 != null) {
            httpClient5.close(CloseMode.GRACEFUL);
        }
    }

    private LayeredConnectionSocketFactory httpsSSLConnectionSocketFactory(boolean isDisableSslValidation) {
        var builder = SSLConnectionSocketFactoryBuilder.create()
                .setTlsVersions(TLS.V_1_3, TLS.V_1_2);

        if (isDisableSslValidation) {
            try {
                var sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, new TrustManager[]{new DisabledValidationTrustManager()}, new SecureRandom());
                builder.setSslContext(sslContext);
                builder.setHostnameVerifier(NoopHostnameVerifier.INSTANCE);
            } catch (Exception e) {
                throw new IllegalStateException("Error creating SSLContext", e);
            }
        } else {
            builder.setSslContext(SSLContexts.createSystemDefault());
        }

        return builder.build();
    }

    private void configureTls(PoolingHttpClientConnectionManagerBuilder builder) {
        if (!properties.isTlsEnabled()) {
            return;
        }

        try {
            var sslContext = createApacheSslContext();
            var hostnameVerifier = properties.isVerifyHostname() ? null : NoopHostnameVerifier.INSTANCE;
            builder.setTlsSocketStrategy(new DefaultClientTlsStrategy(sslContext, hostnameVerifier));
        } catch (Exception e) {
            throw new IllegalStateException("TLS config failed for: " + properties.getClientName(), e);
        }
    }

    private SSLContext createApacheSslContext() throws Exception {
        var ssl = SSLContextBuilder.create();

        var keyStore = properties.getKeyStore();
        if (keyStore != null) {
            var resource = resourceLoader.getResource(keyStore);
            var pwd = properties.getKeyStorePassword();
            ssl.loadKeyMaterial(resource.getURL(), pwd, pwd);
        }

        var trustStore = properties.getTrustStore();
        if (trustStore != null) {
            var resource = resourceLoader.getResource(trustStore);
            var pwd = properties.getTrustStorePassword();
            ssl.loadTrustMaterial(resource.getURL(), pwd);
        }

        return ssl.build();
    }
}
