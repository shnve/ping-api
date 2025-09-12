package fr.shom.ping.model.tuple;

import fr.shom.ping.model.enums.SurveyTransmissionStatus;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static fr.shom.ping.utils.SurveyUtil.getDataDeletionDate;

@Data
@Getter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
public class SurveyTableTuple implements Serializable {
    @Serial
    private static final long serialVersionUID = 8985130964225557459L;

    protected UUID id;

    protected String identifier;

    protected String nameAndLocation;

    protected Instant lastUpdate;

    protected Instant startDate;

    protected Instant endDate;

    protected String compliant;

    protected Instant expectedDeletionDate;

    protected String testProcessingStatus;

    protected Instant testProcessingDate;

    protected String transmissionStatus;

    protected Instant lastUpdateTransmissionDate;

    public SurveyTableTuple(UUID id, String identifier, String nameAndLocation, Instant lastUpdate, Instant startDate, Instant endDate, String compliant,
        String testProcessingStatus, Instant testProcessingDate, String transmissionStatus, Instant lastUpdateTransmissionDate) {
        this.id = id;
        this.identifier = identifier;
        this.nameAndLocation = nameAndLocation;
        this.lastUpdate = lastUpdate;
        this.startDate = startDate;
        this.endDate = endDate;
        this.compliant = compliant;
        this.testProcessingStatus = testProcessingStatus;
        this.testProcessingDate = testProcessingDate;
        this.transmissionStatus = transmissionStatus;
        this.lastUpdateTransmissionDate = lastUpdateTransmissionDate;
        this.expectedDeletionDate = getDataDeletionDate(lastUpdate, SurveyTransmissionStatus.valueOf(transmissionStatus) == SurveyTransmissionStatus.FINISHED);
    }
}
