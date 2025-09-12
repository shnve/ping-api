package fr.shom.ping.repository;

import fr.shom.ping.model.Attachment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {
    List<Attachment> findAllByIdIn(List<UUID> ids);
}
