package com.sportrental.equipment.equipment;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class EquipmentRepository {

    private static final String COLUMNS =
            "id, name, category, daily_price, total_quantity, available_quantity, created_at";

    private static final RowMapper<Equipment> ROW_MAPPER = (rs, rowNum) -> new Equipment(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("category"),
            rs.getBigDecimal("daily_price"),
            rs.getInt("total_quantity"),
            rs.getInt("available_quantity"),
            rs.getObject("created_at", OffsetDateTime.class)
    );

    private final JdbcTemplate jdbc;

    public EquipmentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Equipment create(String name, String category, BigDecimal dailyPrice, int totalQuantity) {
        Long id = jdbc.queryForObject("""
                INSERT INTO equipment_service.equipment
                    (name, category, daily_price, total_quantity, available_quantity)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id
                """, Long.class, name, category, dailyPrice, totalQuantity, totalQuantity);
        return findById(id).orElseThrow(() -> new IllegalStateException("Inserted equipment not found: " + id));
    }

    public Optional<Equipment> findById(long id) {
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM equipment_service.equipment WHERE id = ?",
                ROW_MAPPER, id
        ).stream().findFirst();
    }

    public List<Equipment> findAll(String category) {
        if (category == null || category.isBlank()) {
            return jdbc.query(
                    "SELECT " + COLUMNS + " FROM equipment_service.equipment ORDER BY id",
                    ROW_MAPPER
            );
        }
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM equipment_service.equipment WHERE lower(category) = lower(?) ORDER BY id",
                ROW_MAPPER, category.trim()
        );
    }

    /**
     * Атомарно уменьшает доступное количество. Возвращает пустой Optional,
     * если позиции нет или на складе недостаточно единиц.
     */
    public Optional<Equipment> reserve(long id, int quantity) {
        return jdbc.query("""
                UPDATE equipment_service.equipment
                   SET available_quantity = available_quantity - ?
                 WHERE id = ?
                   AND available_quantity >= ?
                RETURNING %s
                """.formatted(COLUMNS), ROW_MAPPER, quantity, id, quantity)
                .stream().findFirst();
    }

    /**
     * Возвращает единицы инвентаря на склад (не больше общего количества).
     */
    public Optional<Equipment> release(long id, int quantity) {
        return jdbc.query("""
                UPDATE equipment_service.equipment
                   SET available_quantity = LEAST(total_quantity, available_quantity + ?)
                 WHERE id = ?
                RETURNING %s
                """.formatted(COLUMNS), ROW_MAPPER, quantity, id)
                .stream().findFirst();
    }
}
