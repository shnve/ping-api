package fr.shom.ping.utils;

import fr.shom.ping.model.dto.MaritimeServiceSelectDto;
import fr.shom.ping.model.dto.NotificationDto;
import fr.shom.ping.model.enums.NotificationType;
import fr.shom.ping.model.tuple.NotificationTableTuple;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

public final class NotificationUtil {
    private NotificationUtil() {}

    public static void map(LinkedHashMap<UUID, NotificationDto> notifications, NotificationTableTuple tuple) {
        final NotificationDto notificationDto = NotificationDto.builder()
            .id(tuple.getId())
            .type(NotificationType.valueOf(tuple.getType()))
            .title(tuple.getTitle())
            .read(tuple.isRead())
            .creationDate(tuple.getCreationDate())
            .readDate(tuple.getReadDate())
            .subscriptionId(tuple.getSubscriptionId())
            .folderId(tuple.getFolderId())
            .service(
                MaritimeServiceSelectDto.builder()
                    .id(tuple.getServiceId())
                    .name(tuple.getServiceName())
                    .internationalName(tuple.getInternationalServiceName())
                    .build()
            )
            .warningId(tuple.getWarningId())
            .georegulationId(tuple.getGeoregulationId())
            .infosourceId(tuple.getInfosourceId())
            .surveyId(tuple.getSurveyId())
            .build();
        notifications.put(tuple.getId(), notificationDto);
    }

    public static List<NotificationDto> mapToNotificationDtoList(List<?> resultList) {
        final LinkedHashMap<UUID, NotificationDto> notifs = new LinkedHashMap<>();
        resultList.forEach(tuple -> map(notifs, (NotificationTableTuple) tuple));
        return new ArrayList<>(notifs.values());
    }
}
