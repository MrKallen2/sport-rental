package com.sportrental.notifications.notification;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    public record CreateNotificationRequest(
            @NotBlank @Size(max = 60) String type,
            @NotBlank @Size(max = 180) String recipient,
            @NotBlank String message
    ) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Notification create(@Valid @RequestBody CreateNotificationRequest request) {
        return service.send(request.type(), request.recipient(), request.message());
    }

    @GetMapping
    public List<Notification> list(@RequestParam(defaultValue = "50") @Min(1) @Max(500) int limit) {
        return service.list(limit);
    }
}
