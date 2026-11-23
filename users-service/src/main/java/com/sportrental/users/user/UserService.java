package com.sportrental.users.user;

import com.sportrental.users.error.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public User create(String name, String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        User user = repository.create(name.trim(), normalizedEmail);
        log.info("User created: id={} email={}", user.id(), user.email());
        return user;
    }

    public User get(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("User with id=" + id + " not found"));
    }

    public List<User> list() {
        List<User> users = repository.findAll();
        log.info("Users listed: count={}", users.size());
        return users;
    }
}
