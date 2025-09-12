package fr.shom.ping.model.dto;

import fr.shom.ping.model.Folder;
import fr.shom.ping.model.Georegulation;
import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.Notification;
import fr.shom.ping.model.Subscription;
import fr.shom.ping.model.Survey;
import fr.shom.ping.model.Warning;
import fr.shom.ping.model.enums.NotificationType;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import fr.shom.ping.validation.NoHtml;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;

import static java.util.Optional.ofNullable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -1755174842034986736L;

    private UUID id;

    private NotificationType type;

    @NoHtml
    private String title;

    private boolean read;

    private Instant creationDate;

    private Instant readDate;

    private UUID subscriptionId;

    private UUID folderId;

    private @Valid MaritimeServiceSelectDto service;

    private UUID warningId;

    private UUID georegulationId;

    private UUID infosourceId;

    private UUID surveyId;

    public NotificationDto(Notification notification) {
        id = notification.getId();
        type = notification.getType();
        title = notification.getTitle();
        read = notification.isRead();
        creationDate = notification.getCreationDate();
        readDate = notification.getReadDate();
        subscriptionId = ofNullable(notification.getSubscription()).map(Subscription::getId).orElse(null);
        folderId = ofNullable(notification.getFolder()).map(Folder::getId).orElse(null);
        service = ofNullable(notification.getService()).map(MaritimeServiceSelectDto::new).orElse(null);
        warningId = ofNullable(notification.getWarning()).map(Warning::getId).orElse(null);
        georegulationId = ofNullable(notification.getGeoregulation()).map(Georegulation::getId).orElse(null);
        infosourceId = ofNullable(notification.getInfosource()).map(Infosource::getId).orElse(null);
        surveyId = ofNullable(notification.getSurvey()).map(Survey::getId).orElse(null);
    }
}
