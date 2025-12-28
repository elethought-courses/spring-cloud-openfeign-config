package tech.elethoughts.courses.cloud.feign.application;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.elethoughts.courses.cloud.feign.domain.Pokemon;
import tech.elethoughts.courses.cloud.feign.infrastructure.PokeApiClientDefault;

@RestController
@RequestMapping("/api/default/pokemon")
public class PokeApiDefaultController {

    private final PokeApiClientDefault client;

    public PokeApiDefaultController(PokeApiClientDefault client) {
        this.client = client;
    }

    @GetMapping("/{name}")
    public Pokemon getByName(@PathVariable String name) {
        return client.getByName(name);
    }
}
