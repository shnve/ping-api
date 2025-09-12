package fr.shom.ping.repository;

import fr.shom.ping.model.Folder;
import fr.shom.ping.model.dto.FolderReferenceDto;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FolderRepository extends JpaRepository<Folder, UUID> {
    @Query(
        nativeQuery = true,
        value = "SELECT f.* " +
            "FROM folder f " +
            "JOIN notion n ON n.folder_id = f.id " +
            "WHERE n.infosource_id = :infosourceId"
    )
    List<Folder> findByInfosourceId(UUID infosourceId);

    @Query(
        nativeQuery = true,
        value = "SELECT f.* " +
            "FROM folder f " +
            "JOIN notion n ON n.folder_id = f.id " +
            "WHERE n.warning_id = :warningId"
    )
    List<Folder> findByWarningId(UUID warningId);

    @Query(
        nativeQuery = true,
        value = "SELECT f.* " +
            "FROM folder f " +
            "JOIN notion n ON n.folder_id = f.id " +
            "WHERE n.georegulation_id = :georegulationId"
    )
    List<Folder> findByGeoregulationId(UUID georegulationId);

    @Query(
        value = "SELECT MAX(f.number) FROM Folder f " +
            "WHERE EXTRACT(YEAR FROM f.creation_date) = :year",
        nativeQuery = true)
    Integer findLastReservedNumberByYear(int year);

    @Query(
        nativeQuery = true,
        value = "SELECT f.id as folder_id, s.number as study_number, sr.type, sr.value " +
            "FROM folder as f " +
            "JOIN study as s ON s.folder_id = f.id " +
            "JOIN study_reference as sr ON sr.study_id = s.id " +
            "WHERE f.id IN :folderIds"
    )
    Set<FolderReferenceDto> findAllReferencesByFolderIds(List<UUID> folderIds);
}
