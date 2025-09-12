package fr.shom.ping.repository;

import fr.shom.ping.model.StaticLogo;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaticLogoRepository extends JpaRepository<StaticLogo, UUID> {
    boolean existsStaticLogoByKey(String key);

    Optional<StaticLogo> findOneByKey(String key);
}
