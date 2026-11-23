package com.sportrental.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * gateway.services.<name> = базовый URL внутреннего сервиса.
 * Значения приходят из docker-compose (environment) и не зашиты в код.
 */
@ConfigurationProperties(prefix = "gateway")
public record GatewayProperties(Map<String, String> services) {

    public GatewayProperties {
        services = services == null ? Map.of() : Map.copyOf(services);
    }
}
