package tech.elethoughts.courses.cloud.feign.application;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.elethoughts.courses.cloud.feign.domain.Pokemon;
import tech.elethoughts.courses.cloud.feign.infrastructure.PokeApiClientHc5;

@RestController
@RequestMapping("/api/hc5/pokemon")
public class PokeApiHc5Controller {

    private final PokeApiClientHc5 client;

    public PokeApiHc5Controller(PokeApiClientHc5 client) {
        this.client = client;
    }

    @GetMapping("/{name}")
    public Pokemon getByName(@PathVariable String name) {
        return client.getByName(name);
    }
}
