package fr.shom.ping.repository;

import fr.shom.ping.model.Georegulation;
import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.dto.MaritimeEntityConcernedDto;
import fr.shom.ping.model.enums.GeoregulationType;
import fr.shom.ping.model.tuple.GeoregulationToDisplayOnCreationMapTuple;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface GeoregulationRepository extends JpaRepository<Georegulation, UUID> {

    @Query(
        "SELECT new fr.shom.ping.model.tuple.GeoregulationToDisplayOnCreationMapTuple(g.id, feature) " +
            "FROM Georegulation g " +
            "JOIN g.parts gp " +
            "JOIN gp.features feature " +
            "WHERE g.issuingService.id = :issuingServiceId " +
            "AND g.type = :georegulationType"
    )
    List<GeoregulationToDisplayOnCreationMapTuple> findAllToDisplayOnCreationMapByServiceAndType(UUID issuingServiceId, GeoregulationType georegulationType);

    @Query(
        "SELECT DISTINCT new fr.shom.ping.model.dto.MaritimeEntityConcernedDto(e.id, e.name, e.internationalName) " +
            "FROM Georegulation g " +
            "JOIN g.entities e"
    )
    List<MaritimeEntityConcernedDto> getAllGeoregulationEntities();

    Optional<Georegulation> findByIdAndIssuingServiceIdIn(UUID georegulationId, List<UUID> serviceIds);

    @Query(
        "SELECT g " +
            "FROM Georegulation g " +
            "WHERE g.id IN" +
            "(SELECT DISTINCT g.id " +
                "FROM Georegulation g " +
                "JOIN Infosource i ON i.georegulation.id = g.id " +
                "LEFT JOIN InfosourceRecipient ir ON ir.infosource.id = i.id " +
                "WHERE i.status <> fr.shom.ping.model.enums.InfosourceStatus.DRAFT " +
                "AND (i.issuingService = :service OR ir.service = :service))"
    )
    List<Georegulation> findByIssuingServiceOrRecipient(MaritimeService service);

    @Modifying
    @Query(
        "UPDATE Georegulation g " +
            "SET g.isNew = FALSE " +
            "WHERE  g.id = :id AND g.status = fr.shom.ping.model.enums.GeoregulationStatus.PUBLISHED"
    )
    int updateNewStatusToFalse(UUID id);

    @Query(value =
        "SELECT g " +
            "FROM Georegulation g " +
            "WHERE g.number.year = :year " +
            "AND g.number.text = :text " +
            "AND g.number.date = :date")
    List<Georegulation> findByNumberExceptAuthorities(Integer year, String text, LocalDate date);

    @Query("SELECT g FROM Georegulation g WHERE g.status = 'PUBLISHED' AND g.isExpired = FALSE")
    List<Georegulation> findAllToCheckLinks();

    @Modifying
    @Query(
        "UPDATE Georegulation g " +
            "SET g.isExpired = :isExpired " +
            "WHERE  g.id = :id AND g.status = fr.shom.ping.model.enums.GeoregulationStatus.PUBLISHED"
    )
    int updateExpiredStatus(UUID id, boolean isExpired);
}
