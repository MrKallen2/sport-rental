package com.sportrental.rentals.rental;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rentals")
public class RentalController {

    private final RentalService service;

    public RentalController(RentalService service) {
        this.service = service;
    }

    public record CreateRentalRequest(
            @NotNull @Min(1) Long userId,
            @NotNull @Min(1) Long equipmentId,
            @NotNull @Min(1) Integer quantity,
            @NotNull @Min(1) Integer days
    ) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Rental create(@Valid @RequestBody CreateRentalRequest request) {
        return service.create(request.userId(), request.equipmentId(), request.quantity(), request.days());
    }

    @GetMapping
    public List<Rental> list(@RequestParam(required = false) Long userId,
                             @RequestParam(required = false) String status) {
        return service.list(userId, status);
    }

    @GetMapping("/{id}")
    public Rental get(@PathVariable long id) {
        return service.get(id);
    }

    @PostMapping("/{id}/return")
    public Rental returnRental(@PathVariable long id) {
        return service.returnRental(id);
    }
}
