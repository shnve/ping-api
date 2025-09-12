package fr.shom.ping.repository;

import fr.shom.ping.model.Study;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyRepository extends JpaRepository<Study, UUID> {
}
