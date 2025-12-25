package tech.elethoughts.courses.cloud.feign.infrastructure;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import tech.elethoughts.courses.cloud.feign.domain.Page;
import tech.elethoughts.courses.cloud.feign.domain.Pokemon;
import tech.elethoughts.courses.cloud.feign.domain.PokemonSummary;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(classes = PokemonClientTest.TestConfig.class)
@ImportAutoConfiguration(FeignAutoConfiguration.class)
class PokemonClientTest {

    @Configuration
    @EnableFeignClients(clients = PokemonClient.class)
    static class TestConfig {
    }

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Autowired
    private PokemonClient pokemonClient;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.openfeign.client.config.pokeapi.url", wireMock::baseUrl);
        registry.add("spring.cloud.openfeign.client.config.pokeapi.connect-timeout", () -> 1000);
        registry.add("spring.cloud.openfeign.client.config.pokeapi.read-timeout", () -> 1000);
        registry.add("spring.cloud.openfeign.client.config.pokeapi.default-request-headers.Accept", () -> "application/json");
        registry.add("spring.cloud.openfeign.client.config.pokeapi.default-request-headers.User-Agent", () -> "spring-cloud-openfeign-demo");
    }

    @Test
    void shouldGetPokemonByName() {
        wireMock.stubFor(get(urlEqualTo("/api/v1/pokemon/pikachu"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {"id": 25, "name": "pikachu", "height": 4, "weight": 60}
                                """)));

        Pokemon pikachu = pokemonClient.getByNameOrId("pikachu");

        assertThat(pikachu)
                .usingRecursiveComparison()
                .isEqualTo(new Pokemon(25L, "pikachu", 4, 60));

        wireMock.verify(getRequestedFor(urlEqualTo("/api/v1/pokemon/pikachu")));
    }

    @Test
    void shouldListPokemon() {
        wireMock.stubFor(get(urlEqualTo("/api/v1/pokemon?limit=3&offset=0"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                    "count": 1302,
                                    "next": "https://pokeapi.co/api/v1/pokemon?offset=3&limit=3",
                                    "previous": null,
                                    "results": [
                                        {"name": "bulbasaur", "url": "https://pokeapi.co/api/v1/pokemon/1/"},
                                        {"name": "ivysaur", "url": "https://pokeapi.co/api/v1/pokemon/2/"},
                                        {"name": "venusaur", "url": "https://pokeapi.co/api/v1/pokemon/3/"}
                                    ]
                                }
                                """)));

        Page<PokemonSummary> page = pokemonClient.list(3, 0);

        assertThat(page)
                .usingRecursiveComparison()
                .isEqualTo(new Page<>(
                        1302,
                        "https://pokeapi.co/api/v1/pokemon?offset=3&limit=3",
                        null,
                        List.of(
                                new PokemonSummary("bulbasaur", "https://pokeapi.co/api/v1/pokemon/1/"),
                                new PokemonSummary("ivysaur", "https://pokeapi.co/api/v1/pokemon/2/"),
                                new PokemonSummary("venusaur", "https://pokeapi.co/api/v1/pokemon/3/")
                        )
                ));

        wireMock.verify(getRequestedFor(urlEqualTo("/api/v1/pokemon?limit=3&offset=0")));
    }

    @Test
    void shouldSendDefaultRequestHeaders() {
        wireMock.stubFor(get(urlEqualTo("/api/v1/pokemon/charmander"))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader("User-Agent", equalTo("spring-cloud-openfeign-demo"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {"id": 4, "name": "charmander", "height": 6, "weight": 85}
                                """)));

        Pokemon charmander = pokemonClient.getByNameOrId("charmander");

        assertThat(charmander.name()).isEqualTo("charmander");

        wireMock.verify(getRequestedFor(urlEqualTo("/api/v1/pokemon/charmander"))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader("User-Agent", equalTo("spring-cloud-openfeign-demo")));
    }

}
