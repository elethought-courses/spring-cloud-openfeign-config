package tech.elethoughts.courses.cloud.feign.infrastructure;

import feign.*;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class FeignClientConfig {

    @Bean
    ErrorDecoder errorDecoder() {
        return new ErrorDecoder() {
            private final ErrorDecoder defaultDecoder = new Default();

            @Override
            public Exception decode(String methodKey, Response response) {
                int status = response.status();
                if (status >= 400 && status < 500) {
                    return new CustomHttpException(status, methodKey, extractBody(response));
                }
                return defaultDecoder.decode(methodKey, response);
            }

            private String extractBody(Response response) {
                if (response.body() == null) return "";
                try (var is = response.body().asInputStream()) {
                    return new String(is.readAllBytes(), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    return "";
                }
            }
        };
    }

    @Bean
    RequestInterceptor correlationIdInterceptor() {
        return template -> {
            if (!template.headers().containsKey("X-Correlation-ID")) {
                template.header("X-Correlation-ID", UUID.randomUUID().toString());
            }
        };
    }

    @Bean
    Retryer retryer() {
        return new Retryer() {
            private final int maxAttempts = 3;
            private final long initialBackoffMs = 100;
            private final long maxBackoffMs = 1000;
            private int attempt = 1;

            @Override
            public void continueOrPropagate(RetryableException e) {
                if (attempt >= maxAttempts) throw e;
                long backoff = Math.min(initialBackoffMs * (1L << (attempt - 1)), maxBackoffMs);
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
                attempt++;
            }

            @Override
            public Retryer clone() {
                return retryer();
            }
        };
    }

    @Bean
    Logger.Level loggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    Request.Options requestOptions() {
        return new Request.Options(5, TimeUnit.SECONDS, 10, TimeUnit.SECONDS, true);
    }
}
