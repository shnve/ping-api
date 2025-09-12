package fr.shom.ping.repository;

import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.dto.AreaServiceDto;
import fr.shom.ping.model.dto.MaritimeServiceAuthorityDto;
import fr.shom.ping.model.dto.MaritimeServiceDto;
import fr.shom.ping.model.dto.MaritimeServiceInfosourceRecipientDto;
import fr.shom.ping.model.dto.MaritimeServiceSelectDto;
import fr.shom.ping.model.enums.MaritimeServiceType;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.locationtech.jts.geom.Geometry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface MaritimeServiceRepository extends JpaRepository<MaritimeService, UUID>, QuerydslPredicateExecutor<MaritimeService> {

    @Query(
        "SELECT new fr.shom.ping.model.dto.MaritimeServiceInfosourceRecipientDto(ms.id, ms.name, ms.type) " +
            "FROM MaritimeService ms " +
            "INNER JOIN MaritimeEntity me ON me.maritimeService = ms AND me.type = 'SERVICE_AREA' " +
            "WHERE intersects(me.geom, :geometry) = true AND ms.type IN :maritimeServiceTypes"
    )
    List<MaritimeServiceInfosourceRecipientDto> findAllByMaritimeServiceTypeInThatIntersectWith(Geometry geometry, Collection<MaritimeServiceType> maritimeServiceTypes);

    @Query(
        "SELECT new fr.shom.ping.model.dto.MaritimeServiceInfosourceRecipientDto(ms.id, ms.name, ms.type) " +
            "FROM MaritimeService ms " +
            "INNER JOIN MaritimeEntity me ON me.maritimeService = ms  AND me.type = 'SERVICE_AREA' " +
            "WHERE ms.type IN :maritimeServiceTypes"
    )
    List<MaritimeServiceInfosourceRecipientDto> findAllByMaritimeServiceTypeIn(Collection<MaritimeServiceType> maritimeServiceTypes);

    @Query(
        "SELECT new fr.shom.ping.model.dto.MaritimeServiceInfosourceRecipientDto(ms.id, ms.name, ms.type) " +
            "FROM MaritimeService ms " +
            "LEFT JOIN MaritimeEntity me ON me.maritimeService = ms " +
            "LEFT JOIN ms.areas a " +
            "WHERE intersects(me.geom, :geometry) = true OR intersects(a.geom, :geometry) = true"
    )
    List<MaritimeServiceInfosourceRecipientDto> findAllAsDtoByAreaOrEntityIntersectWith(Geometry geometry);

    @Query(
        "SELECT ms " +
            "FROM MaritimeService ms " +
            "LEFT JOIN MaritimeEntity me ON me.maritimeService = ms " +
            "LEFT JOIN ms.areas a " +
            "WHERE intersects(me.geom, :geometry) = true OR intersects(a.geom, :geometry) = true"
    )
    List<MaritimeService> findAllByAreaOrEntityIntersectWith(Geometry geometry);

    @Query(
        "SELECT new fr.shom.ping.model.dto.MaritimeServiceInfosourceRecipientDto(ms.id, ms.name, ms.type) " +
            "FROM InfosourceRecipient ir " +
            "INNER JOIN Infosource i ON ir.infosource.id = i.id " +
            "INNER JOIN MaritimeService ms ON ir.service.id = ms.id " +
            "WHERE i.status <> fr.shom.ping.model.enums.InfosourceStatus.DRAFT " +
            "AND i.issuingService.id = :issuingServiceId " +
            "GROUP BY ms.id, ms.name"
    )
    List<MaritimeServiceInfosourceRecipientDto> findAllRecipientsFromSentInfosourcesWithIssuingServiceId(UUID issuingServiceId);

    @Query(
        "SELECT new fr.shom.ping.model.dto.MaritimeServiceInfosourceRecipientDto(ms.id, ms.name, ms.type) " +
            "FROM InfosourceRecipient ir " +
            "INNER JOIN MaritimeService ms ON ir.service.id = ms.id " +
            "WHERE ir.infosource.id IN (" +
            "SELECT ir.infosource.id " +
            "FROM InfosourceRecipient ir " +
            "INNER JOIN MaritimeService ms ON ir.service.id = ms.id " +
            "WHERE ir.service.id = :issuingServiceId " +
            "GROUP BY ir.infosource.id" +
            ") " +
            "GROUP BY ms.id, ms.name"
    )
    List<MaritimeServiceInfosourceRecipientDto> findAllRecipientsFromSentInfosourcesWithIssuingServiceIdInRecipient(UUID issuingServiceId);

    List<MaritimeServiceDto> findAllByIdIn(List<UUID> serviceIds);

    List<MaritimeService> findByIdIn(Collection<UUID> serviceIds);

    @Query(
        "SELECT DISTINCT new fr.shom.ping.model.dto.AreaServiceDto(a.id, a.name, a.internationalName) " +
            "FROM Area a " +
            "WHERE a.id IN (" +
            "SELECT a.id " +
            "FROM MaritimeService ms " +
            "JOIN ms.areas a " +
            "WHERE ms.id IN :serviceIds)"
    )
    List<AreaServiceDto> findAreasByServiceIdIn(List<UUID> serviceIds);

    @Query("SELECT s.sissyId FROM MaritimeService s WHERE s.sissyId IS NOT NULL")
    List<String> findAllSissyIds();

    MaritimeService findBySissyId(String sissyId);

    @Query(
        "SELECT DISTINCT m " +
            "FROM MaritimeService m " +
            "JOIN m.areas a " +
            "WHERE m.type = fr.shom.ping.model.enums.MaritimeServiceType.AUTHORITY " +
            "AND m.id IN (:authorityIds) " +
            "AND a.id IN (:areaIds)"
    )
    List<MaritimeService> findAuthoritiesByIdsAndAreaIds(List<UUID> authorityIds, List<UUID> areaIds);

    @Query(
        "SELECT DISTINCT new fr.shom.ping.model.dto.MaritimeServiceAuthorityDto(m.id, m.name, m.internationalName, m.authorityCode) " +
            "FROM MaritimeService m " +
            "JOIN m.areas a " +
            "WHERE m.type = fr.shom.ping.model.enums.MaritimeServiceType.AUTHORITY " +
            "AND a.id IN (:areaIds)"
    )
    List<MaritimeServiceAuthorityDto> findAuthoritiesByAreaIds(List<UUID> areaIds);

    @Query(
        "SELECT DISTINCT new fr.shom.ping.model.dto.MaritimeServiceInfosourceRecipientDto(ms.id, ms.name, ms.type) " +
            "FROM MaritimeService ms " +
            "JOIN ms.areas a " +
            "WHERE ms.id <> :id " +
            "AND ms.id NOT IN (:excludedIds) " +
            "AND a.id IN (SELECT ia.id FROM MaritimeService i JOIN i.areas ia WHERE i.id = :id)"
    )
    List<MaritimeServiceInfosourceRecipientDto> findRecipientsWithAreasInServiceAreasExcludingIds(UUID id, Set<UUID> excludedIds);

    @Query(
        "SELECT DISTINCT new fr.shom.ping.model.dto.MaritimeServiceSelectDto(ms.id, ms.name, ms.internationalName) " +
            "FROM MaritimeService ms " +
            "JOIN ms.areas a " +
            "WHERE ms.type = fr.shom.ping.model.enums.MaritimeServiceType.AUTHORITY " +
            "AND a.id IN (SELECT ia.id FROM MaritimeService i JOIN i.areas ia WHERE i.id = :issuingServiceId)"
    )
    List<MaritimeServiceSelectDto> findAuthoritiesWithAreasInIssuingServiceAreas(UUID issuingServiceId);

    @Query(
        "SELECT DISTINCT new fr.shom.ping.model.dto.MaritimeServiceSelectDto(ms.id, ms.name, ms.internationalName) " +
            "FROM ServiceUser s " +
            "JOIN s.roles sr " +
            "JOIN MaritimeService ms ON ms.id = sr.serviceId " +
            "WHERE ms.type = fr.shom.ping.model.enums.MaritimeServiceType.AUTHORITY " +
            "AND s.email = :username"
    )
    List<MaritimeServiceSelectDto> findAuthoritiesByUsername(String username);

    @Query(
        "SELECT DISTINCT ms.id " +
            "FROM ServiceUser s " +
            "JOIN s.roles sr " +
            "JOIN MaritimeService ms ON ms.id = sr.serviceId " +
            "WHERE ms.folderManager = true " +
            "AND s.email = :username " +
            "AND sr.serviceRole IN (fr.shom.ping.model.enums.ServiceRole.SERVICE_OPERATOR, fr.shom.ping.model.enums.ServiceRole.SERVICE_ADMIN)"
    )
    List<UUID> findFolderManagerByServiceUsernameWithOperatorOrAdminRole(String username);

    @Query(
        "SELECT CASE WHEN COUNT(ms.id) > 0 THEN true ELSE false END FROM MaritimeService ms WHERE ms.id IN (:ids) AND ms.folderManager = true"
    )
    boolean hasAnyFolderManagerByIds(List<UUID> ids);

    List<MaritimeService> findAllByTypeIn(MaritimeServiceType... type);

    Set<MaritimeService> findAllByType(MaritimeServiceType type);
}
