package fr.shom.ping.repository;

import fr.shom.ping.model.MaritimeEntity;
import fr.shom.ping.model.dto.MaritimeEntityDto;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface MaritimeEntityRepository extends JpaRepository<MaritimeEntity, UUID>, QuerydslPredicateExecutor<MaritimeEntity> {
    @Query("SELECT e.sissyId FROM MaritimeEntity e WHERE e.sissyId IS NOT NULL")
    List<String> findAllSissyIds();

    @Query("SELECT e FROM MaritimeEntity e WHERE e.sissyId IN (:sissyIds)")
    List<MaritimeEntity> findAllBySissyIds(List<String> sissyIds);

    @Query("SELECT DISTINCT e FROM MaritimeEntity e " +
        "JOIN e.areas a " +
        "WHERE a.id IN (SELECT a.id FROM MaritimeService ms JOIN ms.areas a WHERE ms.id = :serviceId)")
    List<MaritimeEntity> findAvailableMaritimeEntitiesToLink(UUID serviceId);

    List<MaritimeEntityDto> findAllByIdIn(List<UUID> entityIds);

    @Query("SELECT ms.observedMaritimeEntities FROM MaritimeService ms WHERE ms.id = :serviceId")
    List<MaritimeEntity> findObservedEntities(UUID serviceId);

    @Query(
        nativeQuery = true,
        value = "SELECT me.* " +
            "FROM maritime_entity me " +
            "WHERE me.maritime_service_id = :serviceId " +
            "UNION " +
            "SELECT me.* " +
            "FROM maritime_entity me " +
            "INNER JOIN maritime_entity_additional_service meas ON meas.maritime_entity_id = me.id " +
            "WHERE meas.maritime_service_id = :serviceId"
    )
    List<MaritimeEntity> findEntitiesManagedOrObservedByService(UUID serviceId);
}
