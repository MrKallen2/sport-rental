package com.sportrental.rentals.client;

import com.sportrental.rentals.dto.UserDto;
import com.sportrental.rentals.error.NotFoundException;
import com.sportrental.rentals.error.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClientException;

@Component
public class UserServiceClient {

    private static final Logger log = LoggerFactory.getLogger(UserServiceClient.class);

    private final RestClient restClient;
    private final String baseUrl;

    public UserServiceClient(RestClient restClient,
                             @Value("${clients.users.url}") String baseUrl) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
    }

    public UserDto getUser(long id) {
        log.info("HTTP -> users-service GET /users/{}", id);
        try {
            UserDto user = restClient.get()
                    .uri(baseUrl + "/users/{id}", id)
                    .retrieve()
                    .body(UserDto.class);
            if (user == null) {
                throw new NotFoundException("User with id=" + id + " not found");
            }
            log.info("HTTP <- users-service GET /users/{}: {} <{}>", id, user.name(), user.email());
            return user;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                throw new NotFoundException("User with id=" + id + " not found in users-service");
            }
            throw new ServiceUnavailableException("users-service returned HTTP " + e.getStatusCode().value());
        } catch (RestClientException e) {
            log.error("users-service is unreachable: {}", e.getMessage());
            throw new ServiceUnavailableException("users-service is unreachable");
        }
    }
}
