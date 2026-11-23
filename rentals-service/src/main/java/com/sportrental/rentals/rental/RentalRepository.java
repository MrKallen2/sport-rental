package com.sportrental.rentals.rental;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class RentalRepository {

    private static final String COLUMNS = """
            id, user_id, user_name, equipment_id, equipment_name,
            quantity, days, total_price, status, created_at, returned_at
            """;

    private static final RowMapper<Rental> ROW_MAPPER = (rs, rowNum) -> new Rental(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getString("user_name"),
            rs.getLong("equipment_id"),
            rs.getString("equipment_name"),
            rs.getInt("quantity"),
            rs.getInt("days"),
            rs.getBigDecimal("total_price"),
            rs.getString("status"),
            rs.getObject("created_at", OffsetDateTime.class),
            rs.getObject("returned_at", OffsetDateTime.class)
    );

    private final JdbcTemplate jdbc;

    public RentalRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Rental create(long userId, String userName, long equipmentId, String equipmentName,
                         int quantity, int days, BigDecimal totalPrice) {
        Long id = jdbc.queryForObject("""
                INSERT INTO rentals_service.rentals
                    (user_id, user_name, equipment_id, equipment_name, quantity, days, total_price, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE')
                RETURNING id
                """, Long.class, userId, userName, equipmentId, equipmentName, quantity, days, totalPrice);
        return findById(id).orElseThrow(() -> new IllegalStateException("Inserted rental not found: " + id));
    }

    public Optional<Rental> findById(long id) {
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM rentals_service.rentals WHERE id = ?",
                ROW_MAPPER, id
        ).stream().findFirst();
    }

    public List<Rental> findAll(Long userId, String status) {
        if (userId != null) {
            return jdbc.query(
                    "SELECT " + COLUMNS + " FROM rentals_service.rentals WHERE user_id = ? ORDER BY id DESC",
                    ROW_MAPPER, userId
            );
        }
        if (status != null && !status.isBlank()) {
            return jdbc.query(
                    "SELECT " + COLUMNS + " FROM rentals_service.rentals WHERE upper(status) = upper(?) ORDER BY id DESC",
                    ROW_MAPPER, status.trim()
            );
        }
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM rentals_service.rentals ORDER BY id DESC",
                ROW_MAPPER
        );
    }

    public Optional<Rental> markReturned(long id) {
        return jdbc.query("""
                UPDATE rentals_service.rentals
                   SET status = 'RETURNED', returned_at = now()
                 WHERE id = ? AND status = 'ACTIVE'
                RETURNING %s
                """.formatted(COLUMNS), ROW_MAPPER, id)
                .stream().findFirst();
    }
}
