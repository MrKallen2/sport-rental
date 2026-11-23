package com.sportrental.gateway;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class GatewayInfoController {

    private final GatewayProperties properties;

    public GatewayInfoController(GatewayProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("service", "api-gateway");
        body.put("time", OffsetDateTime.now().toString());
        return body;
    }

    @GetMapping("/")
    public Map<String, Object> info() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", "sport-rental api-gateway");
        body.put("description", "Прокат спортивного инвентаря: единая точка входа");
        body.put("routes", Map.of(
                "/api/users/**", properties.services().get("users"),
                "/api/equipment/**", properties.services().get("equipment"),
                "/api/rentals/**", properties.services().get("rentals"),
                "/api/notifications/**", properties.services().get("notifications")
        ));
        return body;
    }
}
