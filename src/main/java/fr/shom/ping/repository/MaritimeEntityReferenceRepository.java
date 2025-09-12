package fr.shom.ping.repository;

import fr.shom.ping.model.MaritimeEntityReference;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MaritimeEntityReferenceRepository extends JpaRepository<MaritimeEntityReference, UUID> {

    @Modifying
    @Query("DELETE FROM MaritimeEntityReference maritimeEntityReference WHERE maritimeEntityReference.id = :id")
    void delete(UUID id);
}
