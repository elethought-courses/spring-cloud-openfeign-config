package tech.elethoughts.courses.cloud.feign.infrastructure;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import tech.elethoughts.courses.cloud.feign.domain.ApiError;
import tech.elethoughts.courses.cloud.feign.domain.Page;
import tech.elethoughts.courses.cloud.feign.domain.Pokemon;
import tech.elethoughts.courses.cloud.feign.domain.PokemonSummary;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ImportAutoConfiguration(FeignAutoConfiguration.class)
abstract class AbstractPokemonClientTest {

    @Configuration
    @EnableFeignClients(clients = PokemonClient.class)
    static class TestConfig {
    }

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Autowired
    protected PokemonClient pokemonClient;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    protected static String wireMockBaseUrl() {
        return wireMock.baseUrl();
    }

    protected ApiError parseErrorBody(FeignException ex) throws JacksonException {
        String body = new String(ex.responseBody().orElseThrow().array(), StandardCharsets.UTF_8);
        return objectMapper.readValue(body, ApiError.class);
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

    @Test
    void shouldHandle400BadRequest() {
        wireMock.stubFor(get(urlEqualTo("/api/v1/pokemon/invalid-pokemon-123"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {"error": "Bad Request", "message": "Invalid pokemon name format", "path": "/api/v1/pokemon/invalid-pokemon-123"}
                                """)));

        assertThatThrownBy(() -> pokemonClient.getByNameOrId("invalid-pokemon-123"))
                .isInstanceOf(FeignException.BadRequest.class)
                .satisfies(ex -> {
                    FeignException feignEx = (FeignException) ex;
                    assertThat(feignEx.status()).isEqualTo(400);

                    ApiError error = parseErrorBody(feignEx);
                    assertThat(error)
                            .usingRecursiveComparison()
                            .isEqualTo(new ApiError("Bad Request", "Invalid pokemon name format", "/api/v1/pokemon/invalid-pokemon-123"));
                });
    }

    @Test
    void shouldHandle401Unauthorized() {
        wireMock.stubFor(get(urlEqualTo("/api/v1/pokemon/arceus"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {"error": "Unauthorized", "message": "Authentication required", "path": "/api/v1/pokemon/arceus"}
                                """)));

        assertThatThrownBy(() -> pokemonClient.getByNameOrId("arceus"))
                .isInstanceOf(FeignException.Unauthorized.class)
                .satisfies(ex -> {
                    FeignException feignEx = (FeignException) ex;
                    assertThat(feignEx.status()).isEqualTo(401);

                    ApiError error = parseErrorBody(feignEx);
                    assertThat(error)
                            .usingRecursiveComparison()
                            .isEqualTo(new ApiError("Unauthorized", "Authentication required", "/api/v1/pokemon/arceus"));
                });
    }

    @Test
    void shouldHandle403Forbidden() {
        wireMock.stubFor(get(urlEqualTo("/api/v1/pokemon/mewtwo"))
                .willReturn(aResponse()
                        .withStatus(403)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {"error": "Forbidden", "message": "Premium subscription required", "path": "/api/v1/pokemon/mewtwo"}
                                """)));

        assertThatThrownBy(() -> pokemonClient.getByNameOrId("mewtwo"))
                .isInstanceOf(FeignException.Forbidden.class)
                .satisfies(ex -> {
                    FeignException feignEx = (FeignException) ex;
                    assertThat(feignEx.status()).isEqualTo(403);

                    ApiError error = parseErrorBody(feignEx);
                    assertThat(error)
                            .usingRecursiveComparison()
                            .isEqualTo(new ApiError("Forbidden", "Premium subscription required", "/api/v1/pokemon/mewtwo"));
                });
    }

    @Test
    void shouldHandle404NotFound() {
        wireMock.stubFor(get(urlEqualTo("/api/v1/pokemon/missingno"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {"error": "Not Found", "message": "Pokemon not found", "path": "/api/v1/pokemon/missingno"}
                                """)));

        assertThatThrownBy(() -> pokemonClient.getByNameOrId("missingno"))
                .isInstanceOf(FeignException.NotFound.class)
                .satisfies(ex -> {
                    FeignException feignEx = (FeignException) ex;
                    assertThat(feignEx.status()).isEqualTo(404);

                    ApiError error = parseErrorBody(feignEx);
                    assertThat(error)
                            .usingRecursiveComparison()
                            .isEqualTo(new ApiError("Not Found", "Pokemon not found", "/api/v1/pokemon/missingno"));
                });
    }

    @Test
    void shouldHandle500InternalServerError() {
        wireMock.stubFor(get(urlEqualTo("/api/v1/pokemon/ditto"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {"error": "Internal Server Error", "message": "Database connection failed", "path": "/api/v1/pokemon/ditto"}
                                """)));

        assertThatThrownBy(() -> pokemonClient.getByNameOrId("ditto"))
                .isInstanceOf(FeignException.InternalServerError.class)
                .satisfies(ex -> {
                    FeignException feignEx = (FeignException) ex;
                    assertThat(feignEx.status()).isEqualTo(500);

                    ApiError error = parseErrorBody(feignEx);
                    assertThat(error)
                            .usingRecursiveComparison()
                            .isEqualTo(new ApiError("Internal Server Error", "Database connection failed", "/api/v1/pokemon/ditto"));
                });
    }
}
