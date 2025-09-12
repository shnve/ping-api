package fr.shom.ping.repository;

import com.querydsl.core.types.Predicate;
import fr.shom.ping.model.Serie;
import fr.shom.ping.model.Warning;
import fr.shom.ping.model.dto.FolderConcernedDto;
import fr.shom.ping.model.dto.MaritimeEntityConcernedDto;
import fr.shom.ping.model.enums.SerieType;
import fr.shom.ping.model.enums.WarningStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface WarningRepository extends JpaRepository<Warning, UUID>, QuerydslPredicateExecutor<Warning> {

    List<Warning> findAll(Predicate predicate);

    Page<Warning> findAll(Predicate predicate, Pageable pageable);

    List<Warning> findByStatus(WarningStatus status);

    @Query(
        "SELECT w.telex " +
            "FROM Warning w " +
            "INNER JOIN Serie s ON w.serie = s " +
            "WHERE s.id = :serieId " +
            "AND w.status = :status " +
            "AND w.telex IS NOT NULL")
    List<String> findByStatusAndSerieIdAndTelexIsNotNull(WarningStatus status, UUID serieId);

    long countByIdInAndStatusIn(List<UUID> ids, List<WarningStatus> statuses);

    long countByIdInAndStatusAndSerie(List<UUID> ids, WarningStatus status, Serie serie);

    int countByStatusAndSerie(WarningStatus status, Serie serie);

    @Query(
        "SELECT COUNT(w.id) " +
            "FROM Warning w " +
            "WHERE w.id IN (SELECT w.id FROM Warning w WHERE w.id IN :ids AND w.serie = :serie AND w.status = :status) " +
            "OR w.id IN (" +
            "SELECT w.id FROM Warning w " +
            "INNER JOIN Serie s ON w.serie = s " +
            "WHERE w.id IN :ids AND s.type = :serieType AND s.maritimeService.id = :maritimeServiceId AND w.status = :status)")
    long countReplacedWarningIds(List<UUID> ids, Serie serie, SerieType serieType, UUID maritimeServiceId, WarningStatus status);

    @Modifying
    @Query("UPDATE Warning w SET w.status = :status WHERE w.id IN :ids")
    void updateStatusByWarningIds(WarningStatus status, List<UUID> ids);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Warning w SET w.status = fr.shom.ping.model.enums.WarningStatus.CANCELLED, w.cancellationDate = :now, w.lastUpdate = :now WHERE w.id IN :id")
    void autoCancel(List<UUID> id, Instant now);

    @Query("SELECT CASE WHEN count(w.id) > 0 " +
        "THEN false " +
        "ELSE true END " +
        "FROM Warning w " +
        "WHERE w.serie = :serie " +
        "AND w.warningNumber = :warningNumber " +
        "AND w.year = :year")
    boolean isWarningNumberAvailable(Serie serie, Integer warningNumber, Integer year);

    @Query("SELECT DISTINCT(w.warningNumber) FROM Warning w WHERE w.serie = :serie AND w.year = :year ORDER BY w.warningNumber")
    List<Integer> findReservedWarningNumbersBySerieAndYear(Serie serie, Integer year);

    @Query("SELECT w FROM Warning w " +
        "WHERE LOWER(w.nameOfSeries) = LOWER(:nameOfSeries) AND w.warningNumber = :warningNumber AND w.year = :year AND w.status = 'PUBLISHED'")
    Optional<Warning> findOneByNameOfSeriesIgnoreCaseAndWarningNumberAndYearAndStatusIsPublished(String nameOfSeries, Integer warningNumber, Integer year);

    @Query("SELECT w FROM Warning w " +
        "WHERE LOWER(w.nameOfSeries) = LOWER(:nameOfSeries) AND CONCAT(w.warningNumber,':',w.year) IN (:warningNumberYearList) AND w.status = 'PUBLISHED'")
    List<Warning> findAllByNameOfSeriesIgnoreCaseAndWarningNumberAndYearListAndStatusIsPublished(String nameOfSeries, List<String> warningNumberYearList);

    @Query(
        "SELECT DISTINCT new fr.shom.ping.model.dto.MaritimeEntityConcernedDto(e.id, e.name, e.internationalName) " +
            "FROM Warning w " +
            "JOIN w.maritimeEntities e"
    )
    List<MaritimeEntityConcernedDto> getAllWarningEntities();

    @Query(
        "SELECT DISTINCT new fr.shom.ping.model.dto.FolderConcernedDto(f) " +
            "FROM Folder f " +
            "JOIN f.notions n ON n.type = fr.shom.ping.model.enums.NotionType.WARNING"
    )
    List<FolderConcernedDto> getAllWarningFolders();

    @Query(
        "SELECT w.id FROM Warning w " +
            "WHERE w.serie = :serie " +
            "AND w.id IN (:warningIdsToBeCancel) " +
            "AND w.status = 'CANCELLED'"
    )
    List<UUID> getCancelledWarningIdsBySerie(List<UUID> warningIdsToBeCancel, Serie serie);

    List<Warning> findAllByStatusAndSerie(WarningStatus status, Serie serie);
}
