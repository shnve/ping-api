package fr.shom.ping.repository;

import fr.shom.ping.model.WarningLink;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface WarningLinkRepository extends JpaRepository<WarningLink, UUID> {

    @Modifying
    @Query("DELETE FROM WarningLink warningLink WHERE warningLink.id = :id")
    void delete(UUID id);
}

