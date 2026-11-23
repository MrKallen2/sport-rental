package com.sportrental.rentals.rental;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record Rental(
        Long id,
        Long userId,
        String userName,
        Long equipmentId,
        String equipmentName,
        int quantity,
        int days,
        BigDecimal totalPrice,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime returnedAt
) {
}
