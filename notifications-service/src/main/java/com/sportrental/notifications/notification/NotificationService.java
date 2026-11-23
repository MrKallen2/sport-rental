package com.sportrental.notifications.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    public Notification send(String type, String recipient, String message) {
        Notification notification = repository.create(type.trim(), recipient.trim(), message);
        // Имитация канала доставки: "отправляем" в консоль.
        log.info("NOTIFICATION SENT id={} type={} to={} message=\"{}\"",
                notification.id(), notification.type(), notification.recipient(), notification.message());
        return notification;
    }

    public List<Notification> list(int limit) {
        List<Notification> notifications = repository.findAll(limit);
        log.info("Notifications listed: count={}", notifications.size());
        return notifications;
    }
}
