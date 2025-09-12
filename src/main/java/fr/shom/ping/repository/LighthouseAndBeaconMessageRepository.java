package fr.shom.ping.repository;

import fr.shom.ping.model.LighthouseAndBeaconMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LighthouseAndBeaconMessageRepository extends JpaRepository<LighthouseAndBeaconMessageEntity, String> {
}
