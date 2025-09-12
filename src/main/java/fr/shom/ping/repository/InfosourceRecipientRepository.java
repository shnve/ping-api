package fr.shom.ping.repository;

import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.InfosourceRecipient;
import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.dto.InfosourceServiceStatusServiceUserDto;
import fr.shom.ping.model.enums.ActionType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface InfosourceRecipientRepository extends JpaRepository<InfosourceRecipient, UUID>, QuerydslPredicateExecutor<InfosourceRecipient> {
    @Query(
        "SELECT ir.service " +
            "FROM InfosourceRecipient ir " +
            "WHERE ir.infosource = :infosource"
    )
    List<MaritimeService> findServicesByInfosource(Infosource infosource);

    @Modifying
    @Query(
        "UPDATE InfosourceRecipient ir " +
            "SET ir.status = fr.shom.ping.model.enums.ActionType.ARCHIVE " +
            "WHERE ir.infosource.id = :infosourceId")
    int archive(UUID infosourceId);

    @Query(
        "SELECT new fr.shom.ping.model.dto.InfosourceServiceStatusServiceUserDto(ir.service.id, ir.service.name, ir.status) " +
            "FROM InfosourceRecipient ir " +
            "WHERE ir.infosource.id = :infosourceId " +
            "AND ir.infosource.issuingPublicUser.id = :issuingPublicUserId"
    )
    List<InfosourceServiceStatusServiceUserDto> findServiceStatusesForPublicUser(UUID infosourceId, UUID issuingPublicUserId);

    @Query(
        "SELECT new fr.shom.ping.model.dto.InfosourceServiceStatusServiceUserDto(ir.service.id, ir.service.name, ir.status) " +
            "FROM InfosourceRecipient ir " +
            "WHERE ir.infosource.id = :infosourceId "
    )
    List<InfosourceServiceStatusServiceUserDto> findServiceStatusesForServiceUser(UUID infosourceId);

    @Modifying
    @Query(
        "UPDATE InfosourceRecipient ir " +
            "SET ir.status = :status " +
            "WHERE ir.infosource.id = :infosourceId " +
            "AND ir.service.id IN (:serviceIds)")
    int updateStatus(UUID infosourceId, List<UUID> serviceIds, ActionType status);

    @Query(
        "SELECT ir.service.id " +
            "FROM InfosourceRecipient ir " +
            "WHERE ir.infosource = :infosource"
    )
    List<UUID> findRecipientServiceIdsByInfosource(Infosource infosource);
}
