package fr.shom.ping.repository;

import fr.shom.ping.model.WarningLocalityNames;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarningLocalityNamesRepository extends JpaRepository<WarningLocalityNames, UUID> {
    List<WarningLocalityNames> findAllByIdIn(List<UUID> ids);
}
