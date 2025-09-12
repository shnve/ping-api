package fr.shom.ping.repository;

import fr.shom.ping.model.StaticSection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaticSectionRepository extends JpaRepository<StaticSection, UUID> {
    Optional<StaticSection> findOneByKey(String key);
}
