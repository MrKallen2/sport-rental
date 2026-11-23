package com.sportrental.users.user;

import java.time.OffsetDateTime;

public record User(
        Long id,
        String name,
        String email,
        OffsetDateTime createdAt
) {
}
