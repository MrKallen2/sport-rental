package com.sportrental.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Простой API Gateway: принимает /api/&lt;service&gt;/** и проксирует запрос
 * в контейнер соответствующего сервиса, добавляя X-Request-Id для сквозного лога.
 */
@RestController
public class GatewayController {

    private static final Logger log = LoggerFactory.getLogger(GatewayController.class);

    private final GatewayProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GatewayController(GatewayProperties properties, RestClient restClient, ObjectMapper objectMapper) {
        this.properties = properties;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @RequestMapping({"/api/{service}", "/api/{service}/**"})
    public ResponseEntity<byte[]> proxy(@PathVariable String service,
                                        HttpServletRequest request) throws IOException {
        String baseUrl = properties.services().get(service);
        if (baseUrl == null) {
            return json(404, Map.of(
                    "status", 404,
                    "error", "Unknown service '" + service + "'",
                    "available", properties.services().keySet()
            ));
        }

        String path = request.getRequestURI().substring("/api".length());
        String queryString = request.getQueryString();
        String targetUrl = baseUrl + path + (queryString == null ? "" : "?" + queryString);

        String incomingRequestId = request.getHeader("X-Request-Id");
        final String requestId = (incomingRequestId == null || incomingRequestId.isBlank())
                ? UUID.randomUUID().toString()
                : incomingRequestId;

        byte[] requestBody = request.getInputStream().readAllBytes();
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        long startedAt = System.currentTimeMillis();

        try {
            RestClient.RequestBodySpec spec = restClient.method(method)
                    .uri(targetUrl)
                    .headers(headers -> {
                        copyIfPresent(request, headers, HttpHeaders.ACCEPT);
                        copyIfPresent(request, headers, HttpHeaders.CONTENT_TYPE);
                        copyIfPresent(request, headers, HttpHeaders.AUTHORIZATION);
                        headers.set("X-Request-Id", requestId);
                    });

            if (requestBody.length > 0) {
                String contentType = request.getContentType();
                spec.contentType(contentType == null
                        ? MediaType.APPLICATION_JSON
                        : MediaType.parseMediaType(contentType));
                spec.body(requestBody);
            }

            ResponseEntity<byte[]> response = spec.exchange((clientRequest, clientResponse) -> {
                HttpHeaders headers = new HttpHeaders();
                String contentType = clientResponse.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
                if (contentType != null) {
                    headers.set(HttpHeaders.CONTENT_TYPE, contentType);
                }
                String location = clientResponse.getHeaders().getFirst(HttpHeaders.LOCATION);
                if (location != null) {
                    headers.set(HttpHeaders.LOCATION, location);
                }
                headers.set("X-Request-Id", requestId);
                headers.set("X-Gateway-Service", service);

                byte[] body = clientResponse.getBody() == null
                        ? new byte[0]
                        : clientResponse.getBody().readAllBytes();
                return ResponseEntity.status(clientResponse.getStatusCode())
                        .headers(headers)
                        .body(body);
            });

            log.info("PROXY {} {} -> {} {} ({} ms) requestId={}",
                    method, path, service, response.getStatusCode().value(),
                    System.currentTimeMillis() - startedAt, requestId);
            return response;

        } catch (RestClientException e) {
            log.error("PROXY FAILED {} {} -> {}: {}", method, path, service, e.getMessage());
            return json(502, Map.of(
                    "status", 502,
                    "error", "Bad Gateway: " + service + " is unavailable",
                    "detail", String.valueOf(e.getMessage())
            ));
        }
    }

    private void copyIfPresent(HttpServletRequest request, HttpHeaders headers, String name) {
        String value = request.getHeader(name);
        if (value != null && !value.isBlank()) {
            headers.set(name, value);
        }
    }

    private ResponseEntity<byte[]> json(int status, Map<String, Object> body) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>(body);
            payload.put("timestamp", java.time.OffsetDateTime.now().toString());
            byte[] bytes = objectMapper.writeValueAsBytes(payload);
            return ResponseEntity.status(status)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.status(status)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"status\":500,\"error\":\"serialization error\"}".getBytes());
        }
    }
}
