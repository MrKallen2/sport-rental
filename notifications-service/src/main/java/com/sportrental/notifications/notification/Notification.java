package com.sportrental.notifications.notification;

import java.time.OffsetDateTime;

public record Notification(
        Long id,
        String type,
        String recipient,
        String message,
        OffsetDateTime createdAt
) {
}
