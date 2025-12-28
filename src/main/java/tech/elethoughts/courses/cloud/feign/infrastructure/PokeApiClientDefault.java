package tech.elethoughts.courses.cloud.feign.infrastructure;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "pokemon-default", configuration = FeignDefaultClientConfig.class)
public interface PokeApiClientDefault extends PokeApiClient {
}
