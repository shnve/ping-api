package fr.shom.ping.repository;

import fr.shom.ping.model.Area;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AreaRepository extends JpaRepository<Area, UUID> {
    boolean existsById(UUID id);
}
