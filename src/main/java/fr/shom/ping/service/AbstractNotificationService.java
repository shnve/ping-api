package fr.shom.ping.service;

import fr.shom.ping.model.Notification;
import fr.shom.ping.repository.NotificationRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

import static java.time.Instant.now;

@RequiredArgsConstructor
public abstract class AbstractNotificationService {
    protected final NotificationRepository repository;

    protected final EntityManager entityManager;

    protected abstract List<Notification> getNotificationsIfBelongToUser(Set<UUID> ids, String username);


    @Transactional
    public void updateReadStatus(Set<UUID> ids, boolean readStatus, String username) {
        List<Notification> notificationsToUpdate = getNotificationsIfBelongToUser(ids, username);
        notificationsToUpdate.forEach(notification -> {
            notification.setRead(readStatus);
            notification.setReadDate(readStatus ? now() : null);
        });
        repository.saveAll(notificationsToUpdate);
    }

    @Transactional
    public void delete(Set<UUID> ids, String username) {
        List<Notification> notificationsToDelete = getNotificationsIfBelongToUser(ids, username);
        repository.deleteAll(notificationsToDelete);
    }

}
