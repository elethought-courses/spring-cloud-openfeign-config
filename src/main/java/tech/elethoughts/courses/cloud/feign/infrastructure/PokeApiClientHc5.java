package tech.elethoughts.courses.cloud.feign.infrastructure;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "pokemon-hc5", configuration = FeignApacheHttpClient5Config.class)
public interface PokeApiClientHc5 extends PokeApiClient {
}
