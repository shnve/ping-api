package fr.shom.ping.repository;

import fr.shom.ping.model.FolderAttachment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FolderAttachmentRepository extends JpaRepository<FolderAttachment, UUID> {

}
