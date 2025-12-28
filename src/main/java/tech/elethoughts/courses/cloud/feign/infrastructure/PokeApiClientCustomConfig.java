package tech.elethoughts.courses.cloud.feign.infrastructure;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "pokemon-custom-config", configuration = FeignClientConfig.class)
public interface PokeApiClientCustomConfig extends PokeApiClient {
}
