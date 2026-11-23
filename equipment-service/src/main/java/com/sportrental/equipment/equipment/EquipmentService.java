package com.sportrental.equipment.equipment;

import com.sportrental.equipment.error.ConflictException;
import com.sportrental.equipment.error.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class EquipmentService {

    private static final Logger log = LoggerFactory.getLogger(EquipmentService.class);

    private final EquipmentRepository repository;

    public EquipmentService(EquipmentRepository repository) {
        this.repository = repository;
    }

    public Equipment create(String name, String category, BigDecimal dailyPrice, int totalQuantity) {
        Equipment equipment = repository.create(name.trim(), category.trim(), dailyPrice, totalQuantity);
        log.info("Equipment created: id={} name={} available={}/{}",
                equipment.id(), equipment.name(), equipment.availableQuantity(), equipment.totalQuantity());
        return equipment;
    }

    public Equipment get(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Equipment with id=" + id + " not found"));
    }

    public List<Equipment> list(String category) {
        List<Equipment> items = repository.findAll(category);
        log.info("Equipment listed: count={} category={}", items.size(), category == null ? "*" : category);
        return items;
    }

    public Equipment reserve(long id, int quantity) {
        Equipment reserved = repository.reserve(id, quantity).orElseThrow(() -> {
            Equipment current = repository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Equipment with id=" + id + " not found"));
            return new ConflictException("Not enough units of '%s': requested=%d, available=%d"
                    .formatted(current.name(), quantity, current.availableQuantity()));
        });
        log.info("Equipment reserved: id={} quantity={} available={}/{}",
                reserved.id(), quantity, reserved.availableQuantity(), reserved.totalQuantity());
        return reserved;
    }

    public Equipment release(long id, int quantity) {
        Equipment released = repository.release(id, quantity)
                .orElseThrow(() -> new NotFoundException("Equipment with id=" + id + " not found"));
        log.info("Equipment released: id={} quantity={} available={}/{}",
                released.id(), quantity, released.availableQuantity(), released.totalQuantity());
        return released;
    }
}
