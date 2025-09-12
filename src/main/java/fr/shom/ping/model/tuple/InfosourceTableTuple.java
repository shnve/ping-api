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
public class InfosourceTableTuple extends AbstractInfosourceTableTuple implements Serializable {
    @Serial
    private static final long serialVersionUID = -8847890900628459837L;

    private UUID issuingPublicUserId;

    private String warningNumber;

    public InfosourceTableTuple(UUID id, String number, String nature, Instant creationDate, Instant lastUpdate, Instant validationTime, String comment,
        String status, UUID issuingPublicUserId, String description, UUID recipientId, String recipientName, String recipientInformation,
        String recipientLocalizedInformation, String actionType, String warningNumber, UUID referenceStudyId, String referenceType, String referenceValue) {
        this.id = id;
        this.number = number;
        this.nature = InfosourceNature.valueOf(nature);
        this.creationDate = creationDate;
        this.lastUpdate = lastUpdate;
        this.validationTime = validationTime;
        this.comment = comment;
        this.status = InfosourceStatus.valueOf(status);
        this.description = description;
        this.issuingPublicUserId = issuingPublicUserId;
        this.recipientId = recipientId;
        this.recipientName = recipientName;
        this.recipientInformation = recipientInformation;
        this.recipientLocalizedInformation = recipientLocalizedInformation;
        if (StringUtils.isNotBlank(actionType)) {
            this.actionType = ActionType.valueOf(actionType);
        }
        this.warningNumber = warningNumber;

        if(referenceStudyId != null) {
            this.referenceStudyId = referenceStudyId;
            this.referenceType = StudyReferencesEnum.valueOf(referenceType);
            this.referenceValue = referenceValue;
        }
    }
}
