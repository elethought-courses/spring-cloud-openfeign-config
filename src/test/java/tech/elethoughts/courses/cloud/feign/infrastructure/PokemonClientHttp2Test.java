package tech.elethoughts.courses.cloud.feign.infrastructure;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = AbstractPokemonClientTest.TestConfig.class)
class PokemonClientHttp2Test extends AbstractPokemonClientTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.openfeign.client.config.pokeapi.url", AbstractPokemonClientTest::wireMockBaseUrl);
        registry.add("spring.cloud.openfeign.client.config.pokeapi.default-request-headers.Accept", () -> "application/json");
        registry.add("spring.cloud.openfeign.client.config.pokeapi.default-request-headers.User-Agent", () -> "spring-cloud-openfeign-demo");

        registry.add("spring.cloud.openfeign.http2client.enabled", () -> true);
        registry.add("spring.cloud.openfeign.httpclient.hc5.enabled", () -> false);
    }
}
