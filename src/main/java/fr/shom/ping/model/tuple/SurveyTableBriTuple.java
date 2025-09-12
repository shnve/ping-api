package fr.shom.ping.model.tuple;

import java.io.Serial;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class SurveyTableBriTuple extends SurveyTableTuple {
    @Serial
    private static final long serialVersionUID = -1458819355341521507L;

    private String shomId;

    private String producerOrganization;

    private String ownerOrganization;

    private String briInternalComment;

    public SurveyTableBriTuple(UUID id, String identifier, String shomId, String producerOrganization, String ownerOrganization, String briInternalComment,
        String nameAndLocation, Instant lastUpdate, Instant startDate, Instant endDate, String compliant, String testProcessingStatus,
        Instant testProcessingDate, String transmissionStatus, Instant lastUpdateTransmissionDate) {
        super(id, identifier, nameAndLocation, lastUpdate, startDate, endDate, compliant, testProcessingStatus, testProcessingDate, transmissionStatus,
            lastUpdateTransmissionDate);
        this.shomId = shomId;
        this.producerOrganization = producerOrganization;
        this.ownerOrganization = ownerOrganization;
        this.briInternalComment = briInternalComment;
    }
}
