package fr.shom.ping.model.tuple;

import fr.shom.ping.model.enums.ActionType;
import fr.shom.ping.model.enums.InfosourceNature;
import fr.shom.ping.model.enums.InfosourceStatus;
import fr.shom.ping.model.enums.StudyReferencesEnum;
import java.time.Instant;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
public abstract class AbstractInfosourceTableTuple {
    protected UUID id;
    protected String number;
    protected InfosourceNature nature;
    protected Instant creationDate;
    protected Instant lastUpdate;
    protected Instant validationTime;
    protected String comment;
    protected InfosourceStatus status;
    protected String description;
    protected UUID recipientId;
    protected String recipientName;
    protected String recipientInformation;
    protected String recipientLocalizedInformation;
    protected ActionType actionType;
    protected UUID referenceStudyId;
    protected StudyReferencesEnum referenceType;
    protected String referenceValue;
}
