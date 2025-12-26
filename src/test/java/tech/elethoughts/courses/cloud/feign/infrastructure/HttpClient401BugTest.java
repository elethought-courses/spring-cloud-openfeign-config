package tech.elethoughts.courses.cloud.feign.infrastructure;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("HTTP Client 401 Error Body Handling - JDK-8052118 Bug Demo")
class HttpClient401BugTest {

    static final String REQUEST_BODY = """
            {"username": "test", "password": "secret"}
            """;

    static final String ERROR_BODY = """
            {"error": "Unauthorized", "message": "Invalid credentials"}
            """;

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    static String wireMockBaseUrl() {
        return wireMock.baseUrl();
    }

    static void stubUnauthorized() {
        wireMock.stubFor(post(urlEqualTo("/login"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(ERROR_BODY)));
    }

    @FeignClient(name = "login-api")
    interface LoginClient {
        @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
        String login(@RequestBody String credentials);
    }

    @Configuration
    @EnableFeignClients(clients = LoginClient.class)
    @ImportAutoConfiguration(FeignAutoConfiguration.class)
    static class TestConfig {
    }

    @Nested
    @SpringJUnitConfig(classes = TestConfig.class)
    @DisplayName("Default Client (HttpURLConnection)")
    class DefaultClientTest {

        @DynamicPropertySource
        static void configure(DynamicPropertyRegistry registry) {
            registry.add("spring.cloud.openfeign.client.config.login-api.url", HttpClient401BugTest::wireMockBaseUrl);
            registry.add("spring.cloud.openfeign.httpclient.hc5.enabled", () -> false);
            registry.add("spring.cloud.openfeign.http2client.enabled", () -> false);
        }

        @Autowired
        LoginClient client;

        @BeforeEach
        void setUp() {
            stubUnauthorized();
        }

        @Test
        @DisplayName("JDK-8052118: POST with body loses 401 response body")
        void shouldLoseBodyDueToJdkBug() {
            assertThatThrownBy(() -> client.login(REQUEST_BODY))
                    .isInstanceOf(FeignException.Unauthorized.class)
                    .satisfies(ex -> {
                        var feignEx = (FeignException) ex;
                        assertThat(feignEx.status()).isEqualTo(401);
                        assertThat(feignEx.contentUTF8()).isEmpty();
                    });
        }
    }

    @Nested
    @SpringJUnitConfig(classes = TestConfig.class)
    @DisplayName("Apache HC5 Client")
    class Hc5ClientTest {

        @DynamicPropertySource
        static void configure(DynamicPropertyRegistry registry) {
            registry.add("spring.cloud.openfeign.client.config.login-api.url", HttpClient401BugTest::wireMockBaseUrl);
            registry.add("spring.cloud.openfeign.httpclient.hc5.enabled", () -> true);
            registry.add("spring.cloud.openfeign.http2client.enabled", () -> false);
        }

        @Autowired
        LoginClient client;

        @BeforeEach
        void setUp() {
            stubUnauthorized();
        }

        @Test
        @DisplayName("Correctly preserves 401 response body")
        void shouldPreserveBody() {
            assertThatThrownBy(() -> client.login(REQUEST_BODY))
                    .isInstanceOf(FeignException.Unauthorized.class)
                    .satisfies(ex -> {
                        var feignEx = (FeignException) ex;
                        assertThat(feignEx.status()).isEqualTo(401);
                        assertThat(feignEx.contentUTF8()).contains("Invalid credentials");
                    });
        }
    }

    @Nested
    @SpringJUnitConfig(classes = TestConfig.class)
    @DisplayName("Java HTTP/2 Client")
    class Http2ClientTest {

        @DynamicPropertySource
        static void configure(DynamicPropertyRegistry registry) {
            registry.add("spring.cloud.openfeign.client.config.login-api.url", HttpClient401BugTest::wireMockBaseUrl);
            registry.add("spring.cloud.openfeign.httpclient.hc5.enabled", () -> false);
            registry.add("spring.cloud.openfeign.http2client.enabled", () -> true);
        }

        @Autowired
        LoginClient client;

        @BeforeEach
        void setUp() {
            stubUnauthorized();
        }

        @Test
        @DisplayName("Correctly preserves 401 response body")
        void shouldPreserveBody() {
            assertThatThrownBy(() -> client.login(REQUEST_BODY))
                    .isInstanceOf(FeignException.Unauthorized.class)
                    .satisfies(ex -> {
                        var feignEx = (FeignException) ex;
                        assertThat(feignEx.status()).isEqualTo(401);
                        assertThat(feignEx.contentUTF8()).contains("Invalid credentials");
                    });
        }
    }
}
