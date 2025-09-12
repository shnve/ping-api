package fr.shom.ping.repository;

import fr.shom.ping.model.Subscription;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
}
