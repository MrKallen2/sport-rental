package com.sportrental.rentals.rental;

import com.sportrental.rentals.client.EquipmentServiceClient;
import com.sportrental.rentals.client.NotificationServiceClient;
import com.sportrental.rentals.client.UserServiceClient;
import com.sportrental.rentals.dto.EquipmentDto;
import com.sportrental.rentals.dto.UserDto;
import com.sportrental.rentals.error.ConflictException;
import com.sportrental.rentals.error.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class RentalService {

    private static final Logger log = LoggerFactory.getLogger(RentalService.class);

    private final RentalRepository repository;
    private final UserServiceClient userServiceClient;
    private final EquipmentServiceClient equipmentServiceClient;
    private final NotificationServiceClient notificationServiceClient;

    public RentalService(RentalRepository repository,
                         UserServiceClient userServiceClient,
                         EquipmentServiceClient equipmentServiceClient,
                         NotificationServiceClient notificationServiceClient) {
        this.repository = repository;
        this.userServiceClient = userServiceClient;
        this.equipmentServiceClient = equipmentServiceClient;
        this.notificationServiceClient = notificationServiceClient;
    }

    /**
     * Сценарий аренды: проверить пользователя -> проверить инвентарь ->
     * зарезервировать единицы -> сохранить аренду -> отправить уведомление.
     */
    public Rental create(long userId, long equipmentId, int quantity, int days) {
        UserDto user = userServiceClient.getUser(userId);
        EquipmentDto equipment = equipmentServiceClient.getEquipment(equipmentId);

        BigDecimal totalPrice = equipment.dailyPrice()
                .multiply(BigDecimal.valueOf(quantity))
                .multiply(BigDecimal.valueOf(days));

        equipmentServiceClient.reserve(equipmentId, quantity);

        Rental rental;
        try {
            rental = repository.create(userId, user.name(), equipmentId, equipment.name(),
                    quantity, days, totalPrice);
        } catch (RuntimeException e) {
            // компенсирующая операция: не оставляем «зависший» резерв
            equipmentServiceClient.release(equipmentId, quantity);
            throw e;
        }

        log.info("Rental #{} created: user={} equipment={} qty={} days={} total={}",
                rental.id(), user.email(), equipment.name(), quantity, days, totalPrice);

        notificationServiceClient.send("RENTAL_CREATED", user.email(),
                "Аренда #%d оформлена: %s, %d шт. на %d дн., сумма %.2f руб."
                        .formatted(rental.id(), equipment.name(), quantity, days, totalPrice));
        return rental;
    }

    public Rental get(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Rental with id=" + id + " not found"));
    }

    public List<Rental> list(Long userId, String status) {
        List<Rental> rentals = repository.findAll(userId, status);
        log.info("Rentals listed: count={} userId={} status={}", rentals.size(), userId, status);
        return rentals;
    }

    public Rental returnRental(long id) {
        Rental rental = get(id);
        if ("RETURNED".equals(rental.status())) {
            throw new ConflictException("Rental #" + id + " is already returned");
        }

        Rental returned = repository.markReturned(id)
                .orElseThrow(() -> new ConflictException("Rental #" + id + " cannot be returned"));

        equipmentServiceClient.release(rental.equipmentId(), rental.quantity());
        log.info("Rental #{} returned: equipment={} qty={}", id, rental.equipmentName(), rental.quantity());

        notificationServiceClient.send("RENTAL_RETURNED", rental.userName(),
                "Аренда #%d закрыта: %s, %d шт. возвращены."
                        .formatted(id, rental.equipmentName(), rental.quantity()));
        return returned;
    }
}
