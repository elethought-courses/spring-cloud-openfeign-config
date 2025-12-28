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

public abstract class AbstractSecurePokemonClientProxyTest {

    protected static void configureProxyProperties(DynamicPropertyRegistry registry, String clientName,
                                                    WireMockExtension target, WireMockExtension proxy) {
        registry.add("spring.cloud.openfeign.client.config." + clientName + ".url", target::baseUrl);
        registry.add("spring.cloud.openfeign.client.config." + clientName + ".proxy.enabled", () -> "true");
        registry.add("spring.cloud.openfeign.client.config." + clientName + ".proxy.host", () -> "localhost");
        registry.add("spring.cloud.openfeign.client.config." + clientName + ".proxy.port", () -> String.valueOf(proxy.getPort()));
    }

    protected static void configureNoProxyProperties(DynamicPropertyRegistry registry, String clientName,
                                                      WireMockExtension server) {
        registry.add("spring.cloud.openfeign.client.config." + clientName + ".url", server::baseUrl);
    }

    @Nested
    abstract class WithProxy {

        @RegisterExtension
        static WireMockExtension proxy = WireMockExtension.newInstance()
                .options(wireMockConfig().dynamicPort().enableBrowserProxying(true))
                .build();

        @RegisterExtension
        static WireMockExtension target = WireMockExtension.newInstance()
                .options(wireMockConfig().dynamicPort())
                .build();

        protected abstract PokeApiClient getClient();

        @BeforeEach
        void setUp() {
            proxy.resetAll();
            target.resetAll();
        }

        @Test
        void shouldRouteRequestThroughProxy() {
            target.stubFor(get("/api/v2/pokemon/pikachu")
                    .willReturn(okJson("""
                            {"id": 25, "name": "pikachu", "height": 4, "weight": 60}
                            """)));

            Pokemon pokemon = getClient().getByName("pikachu");

            assertThat(pokemon.name()).isEqualTo("pikachu");
            proxy.verify(getRequestedFor(urlEqualTo("/api/v2/pokemon/pikachu")));
        }
    }

    @Nested
    abstract class WithoutProxy {

        @RegisterExtension
        static WireMockExtension server = WireMockExtension.newInstance()
                .options(wireMockConfig().dynamicPort())
                .build();

        protected abstract PokeApiClient getClient();

        @BeforeEach
        void setUp() {
            server.resetAll();
        }

        @Test
        void shouldWorkWithoutProxy() {
            server.stubFor(get("/api/v2/pokemon/pikachu")
                    .willReturn(okJson("""
                            {"id": 25, "name": "pikachu", "height": 4, "weight": 60}
                            """)));

            Pokemon pokemon = getClient().getByName("pikachu");

            assertThat(pokemon.name()).isEqualTo("pikachu");
        }
    }
}
