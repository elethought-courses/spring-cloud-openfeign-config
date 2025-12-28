package tech.elethoughts.courses.cloud.feign.application;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.elethoughts.courses.cloud.feign.domain.Pokemon;
import tech.elethoughts.courses.cloud.feign.infrastructure.PokeApiClientHttp2;

@RestController
@RequestMapping("/api/http2/pokemon")
public class PokeApiHttp2Controller {

    private final PokeApiClientHttp2 client;

    public PokeApiHttp2Controller(PokeApiClientHttp2 client) {
        this.client = client;
    }

    @GetMapping("/{name}")
    public Pokemon getByName(@PathVariable String name) {
        return client.getByName(name);
    }
}
