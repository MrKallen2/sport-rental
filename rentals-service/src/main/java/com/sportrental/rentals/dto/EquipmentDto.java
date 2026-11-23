package com.sportrental.rentals.dto;

import java.math.BigDecimal;

public record EquipmentDto(
        Long id,
        String name,
        String category,
        BigDecimal dailyPrice,
        int totalQuantity,
        int availableQuantity
) {
}
