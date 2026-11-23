package com.sportrental.notifications.notification;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class NotificationRepository {

    private static final RowMapper<Notification> ROW_MAPPER = (rs, rowNum) -> new Notification(
            rs.getLong("id"),
            rs.getString("type"),
            rs.getString("recipient"),
            rs.getString("message"),
            rs.getObject("created_at", OffsetDateTime.class)
    );

    private final JdbcTemplate jdbc;

    public NotificationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Notification create(String type, String recipient, String message) {
        Long id = jdbc.queryForObject("""
                INSERT INTO notifications_service.notifications (type, recipient, message)
                VALUES (?, ?, ?)
                RETURNING id
                """, Long.class, type, recipient, message);
        return findById(id).orElseThrow(() -> new IllegalStateException("Inserted notification not found: " + id));
    }

    public Optional<Notification> findById(long id) {
        return jdbc.query(
                "SELECT id, type, recipient, message, created_at FROM notifications_service.notifications WHERE id = ?",
                ROW_MAPPER, id
        ).stream().findFirst();
    }

    public List<Notification> findAll(int limit) {
        return jdbc.query(
                "SELECT id, type, recipient, message, created_at FROM notifications_service.notifications ORDER BY id DESC LIMIT ?",
                ROW_MAPPER, limit
        );
    }
}
