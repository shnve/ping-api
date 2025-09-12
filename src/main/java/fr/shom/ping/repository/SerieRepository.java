package fr.shom.ping.repository;

import fr.shom.ping.model.Serie;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SerieRepository extends JpaRepository<Serie, UUID> {
    Optional<Serie> findOneByNameIgnoreCase(String name);

    List<Serie> findAllByMaritimeServiceIdIn(List<UUID> serviceIds);

    @Query(
        "SELECT s " +
            "FROM Serie s " +
            "INNER JOIN MaritimeService ms ON ms.id = s.maritimeService.id " +
            "INNER JOIN ServiceUser u ON u.id = :serviceUserId " +
            "INNER JOIN u.roles r ON ms.id = r.serviceId " +
            "WHERE s.id = :id " +
            "AND r.serviceRole IN ( " +
            "   fr.shom.ping.model.enums.ServiceRole.SERVICE_ADMIN, " +
            "   fr.shom.ping.model.enums.ServiceRole.SERVICE_OPERATOR " +
            ") "
    )
    Serie findByIdAndUserWithAdminOrOperatorRole(UUID id, UUID serviceUserId);

    @Query(
        nativeQuery = true,
        value = "SELECT DISTINCT * " +
            "FROM serie s " +
            "INNER JOIN serie_sub_area ssa ON ssa.serie_id = s.id " +
            "WHERE ssa.sub_area_id = :subAreaId"
    )
    Set<Serie> findAllBySubArea(UUID subAreaId);

    @Query(
        nativeQuery = true,
        value = "SELECT CASE WHEN :serviceInstanceId IN\n" +
            "(" +
            "    SELECT LOWER(REPLACE(s.name, ' ', '_') || '_' || REGEXP_SPLIT_TO_TABLE(sml.mandatory_languages, ' ')) " +
            "    FROM serie s\n" +
            "             INNER JOIN serie_mandatory_languages sml ON s.id = sml.serie_id " +
            "    ) " +
            "THEN true " +
            "ELSE false END;"
    )
    boolean checkIfServiceInstanceIsAvailable(String serviceInstanceId);
}
