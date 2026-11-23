package com.sportrental.equipment.equipment;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record Equipment(
        Long id,
        String name,
        String category,
        BigDecimal dailyPrice,
        int totalQuantity,
        int availableQuantity,
        OffsetDateTime createdAt
) {
}
