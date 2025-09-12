package fr.shom.ping.repository;

import fr.shom.ping.model.StaticFile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaticFileRepository extends JpaRepository<StaticFile, UUID> {
    boolean existsStaticFileByPath(String path);

    Optional<StaticFile> findOneByPath(String path);
}
