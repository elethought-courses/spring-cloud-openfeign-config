package tech.elethoughts.courses.cloud.feign.infrastructure;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import tech.elethoughts.courses.cloud.feign.domain.Pokemon;

public interface PokeApiClient {

    @GetMapping("/api/v2/pokemon/{name}")
    Pokemon getByName(@PathVariable("name") String name);
}
