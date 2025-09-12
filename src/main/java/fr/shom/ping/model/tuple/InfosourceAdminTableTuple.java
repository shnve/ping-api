package fr.shom.ping.model.tuple;

import fr.shom.ping.model.enums.ActionType;
import fr.shom.ping.model.enums.InfosourceNature;
import fr.shom.ping.model.enums.InfosourceStatus;
import fr.shom.ping.model.enums.StudyReferencesEnum;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
@NoArgsConstructor
public class InfosourceAdminTableTuple extends AbstractInfosourceTableTuple implements Serializable {
    @Serial
    private static final long serialVersionUID = -8110936260840015637L;

    private String emitter;

    private ActionType serviceStatus;

    private UUID issuingServiceId;

    private UUID issuingServiceUserId;

    private String transferredBy;

    private UUID entityId;

    private String entityName;

    private String serviceTag;

    private String folderNumber;

    private String warningNumber;

    public InfosourceAdminTableTuple(UUID id, String number, String nature, Instant creationDate, Instant lastUpdate, Instant validationTime,
        String comment, String status, UUID issuingServiceId, UUID issuingServiceUserId, String description, String actionType, UUID recipientId,
        String recipientName, String recipientInformation, String recipientLocalizedInformation, String transferredBy,
        String emitter, String serviceStatus, UUID entityId, String entityName, String serviceTag, String folderNumber, String warningNumber, UUID referenceStudyId, String referenceType, String referenceValue) {
        this.id = id;
        this.number = number;
        this.nature = InfosourceNature.valueOf(nature);
        this.creationDate = creationDate;
        this.lastUpdate = lastUpdate;
        this.validationTime = validationTime;
        this.comment = comment;
        this.status = InfosourceStatus.valueOf(status);
        this.description = description;
        this.issuingServiceId = issuingServiceId;
        this.issuingServiceUserId = issuingServiceUserId;
        this.recipientId = recipientId;
        this.recipientName = recipientName;
        this.recipientInformation = recipientInformation;
        this.recipientLocalizedInformation = recipientLocalizedInformation;
        if (StringUtils.isNotBlank(actionType)) {
            this.actionType = ActionType.valueOf(actionType);
        }
        this.emitter = emitter;
        if (StringUtils.isNotBlank(serviceStatus)) {
            this.serviceStatus = ActionType.valueOf(serviceStatus);
        }
        this.transferredBy = transferredBy;
        this.entityId = entityId;
        this.entityName = entityName;
        this.serviceTag = serviceTag;
        this.folderNumber = folderNumber;
        this.warningNumber = warningNumber;

        if(referenceStudyId != null) {
            this.referenceStudyId = referenceStudyId;
            this.referenceType = StudyReferencesEnum.valueOf(referenceType);
            this.referenceValue = referenceValue;
        }
    }
}
