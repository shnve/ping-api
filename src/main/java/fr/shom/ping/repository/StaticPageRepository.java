package fr.shom.ping.repository;

import fr.shom.ping.model.StaticPage;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaticPageRepository extends JpaRepository<StaticPage, UUID> {
    boolean existsStaticPageByPath(String path);

    Optional<StaticPage> findOneByPath(String path);
}
