package fr.shom.ping.repository;

import com.querydsl.core.types.Predicate;
import fr.shom.ping.model.SubArea;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface SubAreaRepository extends JpaRepository<SubArea, UUID>, QuerydslPredicateExecutor<SubArea> {

    @Override
    List<SubArea> findAll(Predicate predicate);

    List<SubArea> findAllByIdIn(List<UUID> ids);

    @Query(
        nativeQuery = true,
        value = "SELECT DISTINCT * FROM sub_area sa " +
            "LEFT JOIN sub_area_relationship sar ON sa.id = sar.sub_areas_id " +
            "WHERE sa.area_id = :id " +
            "   OR sar.parent_id IN (" +
            "    SELECT sa.id " +
            "    FROM sub_area sa " +
            "    WHERE sa.area_id = :id" +
            ")"
    )
    List<SubArea> findAllByAreaId (UUID id);
}
