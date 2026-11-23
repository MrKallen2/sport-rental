package com.sportrental.rentals.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Component
public class NotificationServiceClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceClient.class);

    private final RestClient restClient;
    private final String baseUrl;

    public NotificationServiceClient(RestClient restClient,
                                     @Value("${clients.notifications.url}") String baseUrl) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
    }

    /**
     * Уведомление — побочный эффект: если сервис недоступен,
     * аренда всё равно должна сохраниться.
     */
    public void send(String type, String recipient, String message) {
        log.info("HTTP -> notifications-service POST /notifications type={}", type);
        try {
            restClient.post()
                    .uri(baseUrl + "/notifications")
                    .body(Map.of("type", type, "recipient", recipient, "message", message))
                    .retrieve()
                    .toBodilessEntity();
            log.info("HTTP <- notifications-service: notification accepted");
        } catch (RestClientException e) {
            log.warn("notifications-service is unavailable, notification skipped: {}", e.getMessage());
        }
    }
}
