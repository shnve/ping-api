package fr.shom.ping.model.tuple;

import fr.shom.ping.model.enums.WarningHazardTypeDetails;
import fr.shom.ping.model.enums.WarningHazardTypeGeneral;
import fr.shom.ping.model.enums.WarningState;
import fr.shom.ping.model.enums.WarningStatus;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static org.springframework.util.StringUtils.hasText;

@Getter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
public class WarningAdminTableTuple implements Serializable {
    @Serial
    private static final long serialVersionUID = -98538876655551581L;

    private UUID id;
    private String nameOfSeries;
    private Integer warningNumber;
    private Integer year;
    private String modelName;
    private UUID generalAreaId;
    private UUID subAreaId;
    private Integer generalAreaPosition;
    private String generalAreaName;
    private String generalAreaInternationalName;
    private UUID localityId;
    private UUID localityParentId;
    private Integer localityPosition;
    private String localityName;
    private String localityInternationalName;
    private WarningHazardTypeGeneral warningHazardTypeGeneral;
    private WarningHazardTypeDetails warningHazardTypeDetail;
    private Instant creationDate;
    private Instant lastUpdate;
    private LocalDate estimatedPublicationDate;
    private Instant publicationDate;
    private Instant cancellationDate;
    private LocalDate endOfBroadcastDate;
    private LocalDate reviewDate;
    private String internalComment;
    private WarningState state;
    private String referenceNameOfSeries;
    private Integer referenceWarningNumber;
    private Integer referenceYear;
    private WarningStatus status;
    private String entity;
    private String folder;

    public WarningAdminTableTuple(UUID id, String nameOfSeries, Integer warningNumber, Integer year, String modelName, UUID generalAreaId, UUID subAreaId,
        Integer generalAreaPosition, String generalAreaName, String generalAreaInternationalName, UUID localityId, UUID localityParentId, Integer localityPosition,
        String localityName, String localityInternationalName, String warningHazardTypeGeneral, String warningHazardTypeDetail, Instant creationDate,
        Instant lastUpdate, LocalDate estimatedPublicationDate, Instant publicationDate, Instant cancellationDate, LocalDate endOfBroadcastDate,
        LocalDate reviewDate, String internalComment, Integer warningStateOrder, String referenceNameOfSeries, Integer referenceWarningNumber,
        Integer referenceYear, String status, String entity, String folder) {
        this.id = id;
        this.nameOfSeries = nameOfSeries;
        this.warningNumber = warningNumber;
        this.year = year;
        this.modelName = modelName;
        this.generalAreaId = generalAreaId;
        this.subAreaId = subAreaId;
        this.generalAreaPosition = generalAreaPosition;
        this.generalAreaName = generalAreaName;
        this.generalAreaInternationalName = generalAreaInternationalName;
        this.localityId = localityId;
        this.localityParentId = localityParentId;
        this.localityPosition = localityPosition;
        this.localityName = localityName;
        this.localityInternationalName = localityInternationalName;
        this.warningHazardTypeGeneral = WarningHazardTypeGeneral.valueOf(warningHazardTypeGeneral);
        if (hasText(warningHazardTypeDetail)) {
            this.warningHazardTypeDetail = WarningHazardTypeDetails.valueOf(warningHazardTypeDetail);
        }
        this.creationDate = creationDate;
        this.lastUpdate = lastUpdate;
        this.estimatedPublicationDate = estimatedPublicationDate;
        this.publicationDate = publicationDate;
        this.cancellationDate = cancellationDate;
        this.endOfBroadcastDate = endOfBroadcastDate;
        this.reviewDate = reviewDate;
        this.internalComment = internalComment;
        this.state = WarningState.values()[warningStateOrder];
        this.referenceNameOfSeries = referenceNameOfSeries;
        this.referenceWarningNumber = referenceWarningNumber;
        this.referenceYear = referenceYear;
        this.status = WarningStatus.valueOf(status);
        this.entity = entity;
        this.folder = folder;
    }
}
