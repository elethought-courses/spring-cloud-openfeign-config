package tech.elethoughts.courses.cloud.feign.infrastructure;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "pokemon-http2", configuration = FeignHttp2ClientConfig.class)
public interface PokeApiClientHttp2 extends PokeApiClient {
}
