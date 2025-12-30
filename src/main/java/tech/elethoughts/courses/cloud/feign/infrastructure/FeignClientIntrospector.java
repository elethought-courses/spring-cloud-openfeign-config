package tech.elethoughts.courses.cloud.feign.infrastructure;

import feign.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class FeignClientIntrospector implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(FeignClientIntrospector.class);

    private final ApplicationContext applicationContext;

    public FeignClientIntrospector(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(String... args) {
        log.info("=== Feign Clients Introspection ===");

        Map<String, Object> feignClients = applicationContext.getBeansWithAnnotation(FeignClient.class);

        feignClients.forEach((_, bean) -> {
            Class<?> clientInterface = findFeignClientInterface(bean);
            if (clientInterface != null) {
                FeignClient annotation = clientInterface.getAnnotation(FeignClient.class);
                logClientInfo(clientInterface, annotation, bean);
            }
        });

        log.info("=== End Feign Clients Introspection ===");
    }

    private void logClientInfo(Class<?> clientInterface, FeignClient annotation, Object proxy) {
        String clientName = annotation.name();
        String configClasses = formatConfigurationClasses(annotation.configuration());
        String httpClientType = extractHttpClientType(proxy);

        log.info("""

                Feign Client: {}
                  Interface: {}
                  HTTP Client: {}
                  Configuration: {}""",
                clientName,
                clientInterface.getSimpleName(),
                httpClientType,
                configClasses);
    }

    private String extractHttpClientType(Object proxy) {
        try {
            if (Proxy.isProxyClass(proxy.getClass())) {
                InvocationHandler handler = Proxy.getInvocationHandler(proxy);

                // Try to find the Client field in the handler
                Client client = findClientInHandler(handler);
                if (client != null) {
                    return resolveHttpClientType(client);
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract HTTP client type: {}", e.getMessage());
        }
        return "Unknown";
    }

    private Client findClientInHandler(Object handler) {
        try {
            // Navigate through the handler to find the Client
            for (Field field : handler.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(handler);

                if (value instanceof Client client) {
                    return client;
                }

                // Check nested objects (like dispatch map values)
                if (value instanceof Map<?, ?> map) {
                    for (Object mapValue : map.values()) {
                        Client nested = findClientInObject(mapValue);
                        if (nested != null) return nested;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error finding client in handler: {}", e.getMessage());
        }
        return null;
    }

    private Client findClientInObject(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Client client) return client;

        try {
            for (Field field : obj.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(obj);
                if (value instanceof Client client) {
                    return client;
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    private String resolveHttpClientType(Client client) {
        if (client == null) {
            return "Default (HttpURLConnection)";
        }

        String className = client.getClass().getName();

        if (className.contains("ApacheHttp5Client") || className.contains("hc5")) {
            return "Apache HttpClient 5";
        } else if (className.contains("Http2Client") || className.contains("java11")) {
            return "Java HTTP/2 Client";
        } else if (className.contains("OkHttpClient")) {
            return "OkHttp";
        } else if (className.contains("Default") || className.contains("Proxied")) {
            return "Default (HttpURLConnection)";
        }

        return client.getClass().getSimpleName();
    }

    private String formatConfigurationClasses(Class<?>[] configs) {
        if (configs == null || configs.length == 0) {
            return "None";
        }
        return Arrays.stream(configs)
                .map(Class::getSimpleName)
                .collect(Collectors.joining(", "));
    }

    private Class<?> findFeignClientInterface(Object proxy) {
        for (Class<?> iface : proxy.getClass().getInterfaces()) {
            if (iface.isAnnotationPresent(FeignClient.class)) {
                return iface;
            }
        }
        return null;
    }
}
