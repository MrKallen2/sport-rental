package com.sportrental.rentals.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportrental.rentals.dto.EquipmentDto;
import com.sportrental.rentals.error.ConflictException;
import com.sportrental.rentals.error.NotFoundException;
import com.sportrental.rentals.error.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Component
public class EquipmentServiceClient {

    private static final Logger log = LoggerFactory.getLogger(EquipmentServiceClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RestClient restClient;
    private final String baseUrl;

    public EquipmentServiceClient(RestClient restClient,
                                  @Value("${clients.equipment.url}") String baseUrl) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
    }

    public EquipmentDto getEquipment(long id) {
        log.info("HTTP -> equipment-service GET /equipment/{}", id);
        try {
            EquipmentDto equipment = restClient.get()
                    .uri(baseUrl + "/equipment/{id}", id)
                    .retrieve()
                    .body(EquipmentDto.class);
            if (equipment == null) {
                throw new NotFoundException("Equipment with id=" + id + " not found");
            }
            log.info("HTTP <- equipment-service: {} (available {}/{})",
                    equipment.name(), equipment.availableQuantity(), equipment.totalQuantity());
            return equipment;
        } catch (RestClientResponseException e) {
            throw translate(e, "Equipment with id=" + id + " not found in equipment-service");
        } catch (RestClientException e) {
            log.error("equipment-service is unreachable: {}", e.getMessage());
            throw new ServiceUnavailableException("equipment-service is unreachable");
        }
    }

    public EquipmentDto reserve(long id, int quantity) {
        log.info("HTTP -> equipment-service POST /equipment/{}/reserve quantity={}", id, quantity);
        try {
            EquipmentDto equipment = restClient.post()
                    .uri(baseUrl + "/equipment/{id}/reserve", id)
                    .body(Map.of("quantity", quantity))
                    .retrieve()
                    .body(EquipmentDto.class);
            log.info("HTTP <- equipment-service reserve: available {}/{}",
                    equipment.availableQuantity(), equipment.totalQuantity());
            return equipment;
        } catch (RestClientResponseException e) {
            throw translate(e, "Equipment with id=" + id + " not found in equipment-service");
        } catch (RestClientException e) {
            log.error("equipment-service is unreachable: {}", e.getMessage());
            throw new ServiceUnavailableException("equipment-service is unreachable");
        }
    }

    public void release(long id, int quantity) {
        log.info("HTTP -> equipment-service POST /equipment/{}/release quantity={}", id, quantity);
        try {
            restClient.post()
                    .uri(baseUrl + "/equipment/{id}/release", id)
                    .body(Map.of("quantity", quantity))
                    .retrieve()
                    .toBodilessEntity();
            log.info("HTTP <- equipment-service release: ok");
        } catch (RestClientException e) {
            // возврат инвентаря не должен «терять» уже сохранённую аренду
            log.warn("equipment-service release failed: {}", e.getMessage());
        }
    }

    private RuntimeException translate(RestClientResponseException e, String notFoundMessage) {
        if (e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
            return new NotFoundException(notFoundMessage);
        }
        if (e.getStatusCode().value() == HttpStatus.CONFLICT.value()) {
            return new ConflictException(errorMessage(e));
        }
        return new ServiceUnavailableException("equipment-service returned HTTP " + e.getStatusCode().value());
    }

    private String errorMessage(RestClientResponseException e) {
        try {
            JsonNode node = MAPPER.readTree(e.getResponseBodyAsString());
            if (node.hasNonNull("error")) {
                return node.get("error").asText();
            }
        } catch (Exception ignored) {
            // тело ответа не JSON — используем общее сообщение
        }
        return "equipment-service returned HTTP " + e.getStatusCode().value();
    }
}
