package com.sportrental.users.user;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private static final RowMapper<User> ROW_MAPPER = (rs, rowNum) -> new User(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("email"),
            rs.getObject("created_at", java.time.OffsetDateTime.class)
    );

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public User create(String name, String email) {
        Long id = jdbc.queryForObject(
                "INSERT INTO users_service.users (name, email) VALUES (?, ?) RETURNING id",
                Long.class,
                name,
                email
        );
        return findById(id).orElseThrow(() -> new IllegalStateException("Inserted user not found: " + id));
    }

    public Optional<User> findById(long id) {
        List<User> users = jdbc.query(
                "SELECT id, name, email, created_at FROM users_service.users WHERE id = ?",
                ROW_MAPPER,
                id
        );
        return users.stream().findFirst();
    }

    public List<User> findAll() {
        return jdbc.query(
                "SELECT id, name, email, created_at FROM users_service.users ORDER BY id",
                ROW_MAPPER
        );
    }
}
