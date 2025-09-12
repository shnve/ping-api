package fr.shom.ping.model.tuple;

import fr.shom.ping.model.enums.FolderStatus;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

@Data
@Getter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
public class FolderTableTuple implements Serializable {
    @Serial
    private static final long serialVersionUID = -1524495031769479677L;

    private UUID id;
    private Integer number;

    protected String issuingService;

    private FolderStatus status;

    private String comment;

    private String object;

    private Instant creationDate;

    private Instant lastUpdate;

    private LocalDate reviewDate;

    private String locality;

    private String areaName;

    private String areaInternationalName;

    private UUID areaId;

    private UUID issuingServiceId;

    private UUID entityId;

    private String entityName;

    private String entityInternationalName;

    private UUID serviceId;

    private String serviceName;

    private UUID studyId;

    private String studyNumber;

    public FolderTableTuple(UUID id, Integer number, String issuingService, String status, String comment, String object, Instant creationDate,
        Instant lastUpdate, LocalDate reviewDate, String locality, String areaName, String areaInternationalName, UUID areaId, UUID issuingServiceId,
        UUID entityId, String entityName, String entityInternationalName, UUID serviceId, String serviceName, UUID studyId, String studyNumber) {
        this.id = id;
        this.number = number;
        if (StringUtils.isNotBlank(status)) {
            this.status = FolderStatus.valueOf(status);
        }
        this.issuingService = issuingService;
        this.comment = comment;
        this.object = object;
        this.creationDate = creationDate;
        this.lastUpdate = lastUpdate;
        this.reviewDate = reviewDate;
        this.locality = locality;
        this.areaName = areaName;
        this.areaInternationalName = areaInternationalName;
        this.areaId = areaId;
        this.issuingServiceId = issuingServiceId;
        this.entityId = entityId;
        this.entityName = entityName;
        this.entityInternationalName = entityInternationalName;
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.studyId = studyId;
        this.studyNumber = studyNumber;
    }
}
