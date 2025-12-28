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
import tech.elethoughts.courses.cloud.feign.domain.Page;
import tech.elethoughts.courses.cloud.feign.domain.Pokemon;
import tech.elethoughts.courses.cloud.feign.domain.PokemonSummary;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ImportAutoConfiguration(FeignAutoConfiguration.class)
abstract class AbstractPokemonClientTest {

    @Configuration
    @EnableFeignClients(clients = PokemonClient.class)
    static class TestConfig {}

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Autowired
    protected PokemonClient pokemonClient;

    protected static String wireMockBaseUrl() {
        return wireMock.baseUrl();
    }

    @Test
    void shouldGetPokemonByName() {
        wireMock.stubFor(get("/api/v1/pokemon/pikachu")
                .willReturn(okJson("""
                        {"id": 25, "name": "pikachu", "height": 4, "weight": 60}
                        """)));

        Pokemon pikachu = pokemonClient.getByNameOrId("pikachu");

        assertThat(pikachu).isEqualTo(new Pokemon(25L, "pikachu", 4, 60));
    }

    @Test
    void shouldListPokemon() {
        wireMock.stubFor(get("/api/v1/pokemon?limit=3&offset=0")
                .willReturn(okJson("""
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
        wireMock.stubFor(get("/api/v1/pokemon/charmander")
                .willReturn(okJson("""
                        {"id": 4, "name": "charmander", "height": 6, "weight": 85}
                        """)));

        pokemonClient.getByNameOrId("charmander");

        wireMock.verify(getRequestedFor(urlEqualTo("/api/v1/pokemon/charmander"))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader("User-Agent", equalTo("spring-cloud-openfeign-demo")));
    }

    @Test
    void shouldAddCorrelationIdHeader() {
        wireMock.stubFor(get("/api/v1/pokemon/eevee")
                .willReturn(okJson("""
                        {"id": 133, "name": "eevee", "height": 3, "weight": 65}
                        """)));

        pokemonClient.getByNameOrId("eevee");

        wireMock.verify(getRequestedFor(urlEqualTo("/api/v1/pokemon/eevee"))
                .withHeader("X-Correlation-ID", matching("[a-f0-9-]{36}")));
    }

    @Test
    void shouldThrowCustomHttpExceptionOn4xx() {
        wireMock.stubFor(get("/api/v1/pokemon/invalid")
                .willReturn(aResponse().withStatus(400).withBody("{}")));

        assertThatThrownBy(() -> pokemonClient.getByNameOrId("invalid"))
                .isInstanceOf(CustomHttpException.class)
                .satisfies(ex -> assertThat(((CustomHttpException) ex).status()).isEqualTo(400));
    }

    @Test
    void shouldThrowCustomHttpExceptionOn404() {
        wireMock.stubFor(get("/api/v1/pokemon/missingno")
                .willReturn(aResponse().withStatus(404).withBody("{}")));

        assertThatThrownBy(() -> pokemonClient.getByNameOrId("missingno"))
                .isInstanceOf(CustomHttpException.class)
                .satisfies(ex -> assertThat(((CustomHttpException) ex).status()).isEqualTo(404));
    }

    @Test
    void shouldDelegateToDefaultDecoderOn5xx() {
        wireMock.stubFor(get("/api/v1/pokemon/ditto")
                .willReturn(aResponse().withStatus(500).withBody("{}")));

        assertThatThrownBy(() -> pokemonClient.getByNameOrId("ditto"))
                .isInstanceOf(FeignException.class);
    }
}
