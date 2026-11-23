package com.sportrental.equipment.equipment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/equipment")
public class EquipmentController {

    private final EquipmentService service;

    public EquipmentController(EquipmentService service) {
        this.service = service;
    }

    public record CreateEquipmentRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 80) String category,
            @NotNull @DecimalMin("0.0") BigDecimal dailyPrice,
            @NotNull @Min(1) Integer totalQuantity
    ) {
    }

    public record QuantityRequest(@NotNull @Min(1) Integer quantity) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Equipment create(@Valid @RequestBody CreateEquipmentRequest request) {
        return service.create(request.name(), request.category(), request.dailyPrice(), request.totalQuantity());
    }

    @GetMapping
    public List<Equipment> list(@RequestParam(required = false) String category) {
        return service.list(category);
    }

    @GetMapping("/{id}")
    public Equipment get(@PathVariable long id) {
        return service.get(id);
    }

    @PostMapping("/{id}/reserve")
    public Equipment reserve(@PathVariable long id, @Valid @RequestBody QuantityRequest request) {
        return service.reserve(id, request.quantity());
    }

    @PostMapping("/{id}/release")
    public Equipment release(@PathVariable long id, @Valid @RequestBody QuantityRequest request) {
        return service.release(id, request.quantity());
    }
}
