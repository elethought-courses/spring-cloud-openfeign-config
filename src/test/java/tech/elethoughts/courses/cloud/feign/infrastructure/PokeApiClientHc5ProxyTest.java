package tech.elethoughts.courses.cloud.feign.infrastructure;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.junit.jupiter.api.Nested;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

class PokeApiClientHc5ProxyTest extends AbstractSecurePokemonClientProxyTest {

    static final String CLIENT_NAME = "pokemon-hc5";

    @Configuration
    @EnableFeignClients(clients = PokeApiClientHc5.class)
    @ImportAutoConfiguration(FeignAutoConfiguration.class)
    static class TestConfig {
    }

    @Nested
    @SpringJUnitConfig(classes = TestConfig.class)
    class WithProxyTest extends WithProxy {

        @Autowired
        PokeApiClientHc5 client;

        @DynamicPropertySource
        static void configureProperties(DynamicPropertyRegistry registry) {
            configureProxyProperties(registry, CLIENT_NAME, target, proxy);
        }

        @Override
        protected PokeApiClient getClient() {
            return client;
        }
    }

    @Nested
    @SpringJUnitConfig(classes = TestConfig.class)
    class WithoutProxyTest extends WithoutProxy {

        @Autowired
        PokeApiClientHc5 client;

        @DynamicPropertySource
        static void configureProperties(DynamicPropertyRegistry registry) {
            configureNoProxyProperties(registry, CLIENT_NAME, server);
        }

        @Override
        protected PokeApiClient getClient() {
            return client;
        }
    }
}
