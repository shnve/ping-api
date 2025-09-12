package fr.shom.ping.model.tuple;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Data
@Getter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
public class NotificationTableTuple implements Serializable {
    @Serial
    private static final long serialVersionUID = -1242803047825568882L;

    private UUID id;

    private String type;

    private String title;

    private boolean read;

    private Instant creationDate;

    private Instant readDate;

    private UUID subscriptionId;

    private UUID folderId;

    private UUID serviceId;

    private String serviceName;

    private String internationalServiceName;

    private UUID warningId;

    private UUID georegulationId;

    private UUID infosourceId;

    private UUID surveyId;

    public NotificationTableTuple(UUID id, String type, String title, boolean read, Instant creationDate, Instant readDate, UUID subscriptionId, UUID folderId, UUID serviceId, String serviceName, String internationalServiceName, UUID warningId, UUID georegulationId, UUID infosourceId, UUID surveyId) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.read = read;
        this.creationDate = creationDate;
        this.readDate = readDate;
        this.subscriptionId = subscriptionId;
        this.folderId = folderId;
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.internationalServiceName = internationalServiceName;
        this.warningId = warningId;
        this.georegulationId = georegulationId;
        this.infosourceId = infosourceId;
        this.surveyId = surveyId;
    }
}
