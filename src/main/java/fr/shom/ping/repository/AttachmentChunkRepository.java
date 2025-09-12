package fr.shom.ping.repository;

import fr.shom.ping.model.AttachmentChunk;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface AttachmentChunkRepository extends JpaRepository<AttachmentChunk, UUID>, JpaSpecificationExecutor<AttachmentChunk> {

    @Modifying
    long deleteByIdentifier(String identifier);
}
