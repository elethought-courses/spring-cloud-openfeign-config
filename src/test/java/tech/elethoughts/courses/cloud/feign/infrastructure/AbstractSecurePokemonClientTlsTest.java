package tech.elethoughts.courses.cloud.feign.infrastructure;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.context.DynamicPropertyRegistry;
import tech.elethoughts.courses.cloud.feign.domain.Pokemon;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public abstract class AbstractSecurePokemonClientTlsTest {

    protected static void configureTlsProperties(DynamicPropertyRegistry registry, String clientName,
                                                  WireMockExtension httpsServer) {
        registry.add("spring.cloud.openfeign.client.config." + clientName + ".url",
                () -> "https://localhost:" + httpsServer.getHttpsPort());
        registry.add("spring.cloud.openfeign.client.config." + clientName + ".tls.enabled", () -> "true");
        registry.add("spring.cloud.openfeign.client.config." + clientName + ".tls.trust-store", () -> "classpath:wiremock-truststore.p12");
        registry.add("spring.cloud.openfeign.client.config." + clientName + ".tls.trust-store-password", () -> "changeit");
        registry.add("spring.cloud.openfeign.client.config." + clientName + ".tls.verify-hostname", () -> "false");
    }

    protected static void configureNoTlsProperties(DynamicPropertyRegistry registry, String clientName,
                                                    WireMockExtension httpsServer) {
        registry.add("spring.cloud.openfeign.client.config." + clientName + ".url",
                () -> "https://localhost:" + httpsServer.getHttpsPort());
    }

    @Nested
    abstract class WithTrustStore {

        @RegisterExtension
        static WireMockExtension httpsServer = WireMockExtension.newInstance()
                .options(wireMockConfig()
                        .dynamicHttpsPort()
                        .httpDisabled(true)
                        .keystorePath("src/test/resources/wiremock-keystore.p12")
                        .keystorePassword("changeit")
                        .keyManagerPassword("changeit")
                        .keystoreType("PKCS12"))
                .build();

        protected abstract PokeApiClient getClient();

        @BeforeEach
        void setUp() {
            httpsServer.resetAll();
        }

        @Test
        void shouldConnectWithCustomTrustStore() {
            httpsServer.stubFor(get("/api/v2/pokemon/mewtwo")
                    .willReturn(okJson("""
                            {"id": 150, "name": "mewtwo", "height": 20, "weight": 1220}
                            """)));

            Pokemon pokemon = getClient().getByName("mewtwo");

            assertThat(pokemon.name()).isEqualTo("mewtwo");
        }
    }

    @Nested
    abstract class WithoutTrustStore {

        @RegisterExtension
        static WireMockExtension httpsServer = WireMockExtension.newInstance()
                .options(wireMockConfig()
                        .dynamicHttpsPort()
                        .httpDisabled(true)
                        .keystorePath("src/test/resources/wiremock-keystore.p12")
                        .keystorePassword("changeit")
                        .keyManagerPassword("changeit")
                        .keystoreType("PKCS12"))
                .build();

        protected abstract PokeApiClient getClient();

        @BeforeEach
        void setUp() {
            httpsServer.resetAll();
        }

        @Test
        void shouldFailWithoutTrustStore() {
            httpsServer.stubFor(get("/api/v2/pokemon/mewtwo")
                    .willReturn(okJson("""
                            {"id": 150, "name": "mewtwo", "height": 20, "weight": 1220}
                            """)));

            assertThatThrownBy(() -> getClient().getByName("mewtwo"))
                    .hasMessageContaining("PKIX path building failed");
        }
    }
}
