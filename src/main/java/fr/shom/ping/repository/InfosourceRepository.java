package fr.shom.ping.repository;

import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.tuple.InfosourceToDisplayOnCreationMapTuple;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface InfosourceRepository extends JpaRepository<Infosource, UUID> {
    @Query(
        "SELECT new fr.shom.ping.model.tuple.InfosourceToDisplayOnCreationMapTuple(i.id, localisation, i.number) " +
            "FROM Infosource i " +
            "JOIN i.infosourceParts ip ON ip.infosource = i " +
            "JOIN ip.localisation localisation " +
            "WHERE i.id != :infosourceId " +
            "AND i.issuingPublicUser.id = :publicUserId"
    )
    List<InfosourceToDisplayOnCreationMapTuple> findAllToDisplayOnCreationMapByPublicUserIdAndExcludedInfosource(UUID infosourceId, UUID publicUserId);

    @Query(
        "SELECT new fr.shom.ping.model.tuple.InfosourceToDisplayOnCreationMapTuple(i.id, localisation, i.number) " +
            "FROM Infosource i " +
            "JOIN i.infosourceParts ip ON ip.infosource = i " +
            "JOIN ip.localisation localisation " +
            "WHERE i.issuingPublicUser.id = :publicUserId"
    )
    List<InfosourceToDisplayOnCreationMapTuple> findAllToDisplayOnCreationMapByPublicUserId(UUID publicUserId);

    @Query(
        "SELECT new fr.shom.ping.model.tuple.InfosourceToDisplayOnCreationMapTuple(i.id, localisation, i.number) " +
            "FROM Infosource i " +
            "JOIN i.infosourceParts ip ON ip.infosource = i " +
            "JOIN ip.localisation localisation " +
            "WHERE i.issuingServiceUser.id = :serviceUserId " +
            "OR i.issuingService = :service"
    )
    List<InfosourceToDisplayOnCreationMapTuple> findAllToDisplayOnCreationMapByServiceUserIdOrService(UUID serviceUserId, MaritimeService service);

    @Query(
        "SELECT new fr.shom.ping.model.tuple.InfosourceToDisplayOnCreationMapTuple(i.id, localisation, i.number) " +
            "FROM Infosource i " +
            "JOIN i.infosourceParts ip ON ip.infosource = i " +
            "JOIN ip.localisation localisation " +
            "WHERE i.id != :infosourceId " +
            "AND (i.issuingServiceUser.id = :serviceUserId " +
            "OR i.issuingService = :service)"
    )
    List<InfosourceToDisplayOnCreationMapTuple> findAllToDisplayOnCreationMapByServiceUserIdOrServiceAndExcludedInfosource(UUID infosourceId,
        UUID serviceUserId,
        MaritimeService service);

    @Query(
        value = "select case when current_number is not null then max(current_number) else 0 end " +
            " from (" +
            "select max(cast(substring(i.number, length(i.number)-6, 2) as integer)) " +
            "FROM infosource i WHERE i.issuing_public_user_id = :userId AND EXTRACT(YEAR FROM i.validation_time) = :year and i.number is not null" +
            ") as current_number",
        nativeQuery = true
    )
    long countReportedInfosourcesByPublicUserWithinYear(UUID userId, int year);

    @Query(
        value = "select case when current_number is not null then max(current_number) else 0 end " +
            " from (" +
            "select max(cast(substring(i.number, length(i.number)-6, 2) as integer)) " +
            "FROM infosource i WHERE i.issuing_service_user_id = :userId AND EXTRACT(YEAR FROM i.validation_time) = :year and i.number is not null" +
            ") as current_number",
        nativeQuery = true
    )
    long countReportedInfosourcesByServiceUserWithinYear(UUID userId, int year);

    @Modifying
    @Query(
        "UPDATE Infosource i " +
            "SET i.status = fr.shom.ping.model.enums.InfosourceStatus.REJECTED, " +
            "i.lastUpdate = :now " +
            "WHERE i.id = :id " +
            "AND i.status NOT IN (" +
            "   fr.shom.ping.model.enums.InfosourceStatus.DRAFT," +
            "   fr.shom.ping.model.enums.InfosourceStatus.REJECTED" +
            ")" )
    int reject(UUID id, Instant now);

    Optional<Infosource> findByIdAndIssuingPublicUserId(UUID infosourceId, UUID issuingPublicUserId);

    Optional<Infosource> findByIdAndIssuingServiceIdIn(UUID infosourceId, List<UUID> serviceIds);

    Optional<Infosource> findByNumber(String number);

    @Query(
        "SELECT i " +
            "FROM Infosource i " +
            "WHERE i.id = :id " +
            "AND i.status = fr.shom.ping.model.enums.InfosourceStatus.DRAFT"
    )
    Optional<Infosource> findByIdAndDraftStatus(UUID id);

    @Query(
        "SELECT i " +
            "FROM Infosource i " +
            "INNER JOIN InfosourceRecipient ir ON ir.infosource.id = i.id " +
            "WHERE i.id = :id " +
            "AND ir.service.id IN (:recipientIds)"
    )
    Optional<Infosource> findByIdAndRecipientIds(UUID id, List<UUID> recipientIds);

    @Query(
        "SELECT i " +
            "FROM Infosource i " +
            "LEFT JOIN InfosourceRecipient ir ON ir.infosource.id = i.id " +
            "WHERE i.status <> fr.shom.ping.model.enums.InfosourceStatus.DRAFT " +
            "AND (i.issuingService = :service OR ir.service = :service)"
    )
    List<Infosource> findByIssuingServiceOrRecipient(MaritimeService service);

    @Query(
        nativeQuery = true,
        value = "select distinct t.value " +
            "from tag t " +
            "where t.maritime_service_id = :issuingServiceId"
    )
    List<String> findTagsByIssuingService(UUID issuingServiceId);

    boolean existsByGeoregulationId(UUID georegulationid);

    boolean existsBySurveyId(UUID surveyId);

    Optional<Infosource> findByGeoregulationId(UUID georegulationId);
}
