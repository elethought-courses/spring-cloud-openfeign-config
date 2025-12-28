package tech.elethoughts.courses.cloud.feign.infrastructure;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import tech.elethoughts.courses.cloud.feign.domain.Pokemon;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringJUnitConfig(classes = FeignClientConfigTest.TestConfig.class)
class FeignClientConfigTest {

    @Configuration
    @EnableFeignClients(clients = PokeApiClientCustomConfig.class)
    @ImportAutoConfiguration(FeignAutoConfiguration.class)
    static class TestConfig {
    }

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Autowired
    PokeApiClientCustomConfig client;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.openfeign.client.config.pokemon-custom-config.url", wireMock::baseUrl);
        registry.add("spring.cloud.openfeign.client.config.pokemon-custom-config.default-request-headers.Accept",
                () -> "application/json");
        registry.add("spring.cloud.openfeign.client.config.pokemon-custom-config.default-request-headers.User-Agent",
                () -> "spring-cloud-openfeign-demo");
    }

    @Test
    void shouldGetPokemonByName() {
        wireMock.stubFor(get("/api/v2/pokemon/pikachu")
                .willReturn(okJson("""
                        {"id": 25, "name": "pikachu", "height": 4, "weight": 60}
                        """)));

        Pokemon pikachu = client.getByName("pikachu");

        assertThat(pikachu).isEqualTo(new Pokemon(25L, "pikachu", 4, 60));
    }

    @Test
    void shouldSendDefaultRequestHeaders() {
        wireMock.stubFor(get("/api/v2/pokemon/charmander")
                .willReturn(okJson("""
                        {"id": 4, "name": "charmander", "height": 6, "weight": 85}
                        """)));

        client.getByName("charmander");

        wireMock.verify(getRequestedFor(urlEqualTo("/api/v2/pokemon/charmander"))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader("User-Agent", equalTo("spring-cloud-openfeign-demo")));
    }

    @Test
    void shouldAddCorrelationIdHeader() {
        wireMock.stubFor(get("/api/v2/pokemon/eevee")
                .willReturn(okJson("""
                        {"id": 133, "name": "eevee", "height": 3, "weight": 65}
                        """)));

        client.getByName("eevee");

        wireMock.verify(getRequestedFor(urlEqualTo("/api/v2/pokemon/eevee"))
                .withHeader("X-Correlation-ID", matching("[a-f0-9-]{36}")));
    }

    @Test
    void shouldThrowCustomHttpExceptionOn4xx() {
        wireMock.stubFor(get("/api/v2/pokemon/invalid")
                .willReturn(aResponse().withStatus(400).withBody("{}")));

        assertThatThrownBy(() -> client.getByName("invalid"))
                .isInstanceOf(CustomHttpException.class)
                .satisfies(ex -> assertThat(((CustomHttpException) ex).status()).isEqualTo(400));
    }

    @Test
    void shouldThrowCustomHttpExceptionOn404() {
        wireMock.stubFor(get("/api/v2/pokemon/missingno")
                .willReturn(aResponse().withStatus(404).withBody("{}")));

        assertThatThrownBy(() -> client.getByName("missingno"))
                .isInstanceOf(CustomHttpException.class)
                .satisfies(ex -> assertThat(((CustomHttpException) ex).status()).isEqualTo(404));
    }

    @Test
    void shouldDelegateToDefaultDecoderOn5xx() {
        wireMock.stubFor(get("/api/v2/pokemon/ditto")
                .willReturn(aResponse().withStatus(500).withBody("{}")));

        assertThatThrownBy(() -> client.getByName("ditto"))
                .isInstanceOf(FeignException.class);
    }
}
