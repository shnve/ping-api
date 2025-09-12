package fr.shom.ping.repository;

import fr.shom.ping.model.Notification;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
}
