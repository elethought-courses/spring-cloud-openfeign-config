# Spring Cloud OpenFeign Configuration Demo

This repository is the companion code for the article: **[Spring Cloud OpenFeign Configuration](https://abissens.elethoughts.tech/blog/articles_spring_cloud_openfeign_config/)**

This project demonstrates advanced Spring Cloud OpenFeign configuration including:
- Multiple HTTP client implementations (Apache HttpClient 5, Java HTTP/2, Default)
- Per-client proxy configuration
- Per-client TLS/SSL configuration with custom truststores
- Custom Feign configuration (error handling, interceptors)

## Prerequisites

- Java 25+
- Maven 3.9+
- Python 3.x (for mitmproxy)

## Project Structure

```
src/main/java/.../infrastructure/
├── PokeApiClient.java                 # Base Feign client interface
├── PokeApiClientHc5.java              # Apache HttpClient 5 implementation
├── PokeApiClientHttp2.java            # Java HTTP/2 implementation
├── PokeApiClientDefault.java          # Default HttpURLConnection implementation
├── PokeApiClientCustomConfig.java     # Custom config (error handling, interceptors)
├── FeignApacheHttpClient5Config.java  # HC5 configuration with proxy/TLS
├── FeignHttp2ClientConfig.java        # HTTP/2 configuration with proxy/TLS
├── FeignDefaultClientConfig.java      # Default client configuration with proxy/TLS
├── FeignClientConfig.java             # Custom Feign config (ErrorDecoder, Interceptors)
└── FeignClientIntrospector.java       # Logs all Feign clients at startup
```

## Quick Start

```bash
# Run without proxy (will fail TLS validation without truststore)
mvn spring-boot:run

# Test endpoints
curl http://localhost:8090/api/hc5/pokemon/pikachu
curl http://localhost:8090/api/http2/pokemon/charizard
curl http://localhost:8090/api/default/pokemon/bulbasaur
```

## Proxy Setup with mitmproxy

### 1. Install mitmproxy

```bash
pip install mitmproxy
```

### 2. Generate mitmproxy CA Certificate

Start mitmproxy once to generate the CA certificate:

```bash
mitmweb --listen-port 8888
```

Press `Ctrl+C` to stop after it starts. The CA certificate is created at:
- **Windows:** `%USERPROFILE%\.mitmproxy\mitmproxy-ca-cert.pem`
- **Linux/macOS:** `~/.mitmproxy/mitmproxy-ca-cert.pem`

### 3. Create Java Truststore

Convert the mitmproxy CA certificate to a Java PKCS12 truststore:

**Windows (Command Prompt):**
```cmd
mkdir certs
keytool -importcert -alias mitmproxy-ca -file "%USERPROFILE%\.mitmproxy\mitmproxy-ca-cert.pem" -keystore certs\mitmproxy-truststore.p12 -storetype PKCS12 -storepass changeit -noprompt
```

**Linux/macOS:**
```bash
mkdir -p certs
keytool -importcert -alias mitmproxy-ca -file ~/.mitmproxy/mitmproxy-ca-cert.pem -keystore certs/mitmproxy-truststore.p12 -storetype PKCS12 -storepass changeit -noprompt
```

Or use the provided scripts:
```bash
# Windows
scripts\setup-mitmproxy.bat

# Linux/macOS
./scripts/setup-mitmproxy.sh
```

### 4. Start mitmproxy

```bash
mitmweb --listen-port 8888
```

This starts:
- Proxy server on port **8888**
- Web UI on port **8081** (http://localhost:8081)

### 5. Run the Application

```bash
mvn spring-boot:run
```

### 6. Test and Observe Traffic

```bash
curl http://localhost:8090/api/hc5/pokemon/pikachu
curl http://localhost:8090/api/http2/pokemon/charizard
curl http://localhost:8090/api/default/pokemon/bulbasaur
```

Open http://localhost:8081 to see all HTTPS traffic intercepted by mitmproxy.

## Configuration

The proxy and TLS settings are configured in `application.yml`:

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          pokemon-hc5:
            url: https://pokeapi.co
            proxy:
              enabled: true
              host: localhost
              port: 8888
            tls:
              enabled: true
              trust-store: file:./certs/mitmproxy-truststore.p12
              trust-store-password: changeit
              verify-hostname: false
```

### Configuration Options

| Property                   | Description                                         |
|----------------------------|-----------------------------------------------------|
| `proxy.enabled`            | Enable/disable proxy for this client                |
| `proxy.host`               | Proxy hostname                                      |
| `proxy.port`               | Proxy port                                          |
| `tls.enabled`              | Enable custom TLS configuration                     |
| `tls.trust-store`          | Path to truststore (supports `file:`, `classpath:`) |
| `tls.trust-store-password` | Truststore password                                 |
| `tls.verify-hostname`      | Enable/disable hostname verification                |

## Disabling Proxy

To run without proxy, set `proxy.enabled: false` for each client or remove the proxy configuration:

```yaml
pokemon-hc5:
  url: https://pokeapi.co
  proxy:
    enabled: false
```

## API Endpoints

| Endpoint                          | HTTP Client                 | Description         |
|-----------------------------------|-----------------------------|---------------------|
| `GET /api/hc5/pokemon/{name}`     | Apache HttpClient 5         | Get Pokemon by name |
| `GET /api/http2/pokemon/{name}`   | Java HTTP/2                 | Get Pokemon by name |
| `GET /api/default/pokemon/{name}` | Default (HttpURLConnection) | Get Pokemon by name |

## Running Tests

```bash
mvn test
```

Tests use WireMock to mock the PokeAPI and test proxy/TLS configurations independently.

## License

[MIT](LICENSE)
