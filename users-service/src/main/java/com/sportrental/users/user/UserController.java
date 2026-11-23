package com.sportrental.users.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    public record CreateUserRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Email @Size(max = 180) String email
    ) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User create(@Valid @RequestBody CreateUserRequest request) {
        return service.create(request.name(), request.email());
    }

    @GetMapping
    public List<User> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public User get(@PathVariable long id) {
        return service.get(id);
    }
}
