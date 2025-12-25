package tech.elethoughts.courses.cloud.feign.infrastructure;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import tech.elethoughts.courses.cloud.feign.domain.Page;
import tech.elethoughts.courses.cloud.feign.domain.Pokemon;
import tech.elethoughts.courses.cloud.feign.domain.PokemonSummary;

@FeignClient(name = "pokeapi")
public interface PokemonClient {

    @GetMapping("/api/v1/pokemon/{nameOrId}")
    Pokemon getByNameOrId(@PathVariable("nameOrId") String nameOrId);

    @GetMapping("/api/v1/pokemon")
    Page<PokemonSummary> list(@RequestParam("limit") int limit, @RequestParam("offset") int offset);
}
