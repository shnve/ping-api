package fr.shom.ping.service;

import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.model.Folder;
import fr.shom.ping.model.Georegulation;
import fr.shom.ping.model.GeoregulationSubscription;
import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.Notification;
import fr.shom.ping.model.PublicUser;
import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.Subscription;
import fr.shom.ping.model.Survey;
import fr.shom.ping.model.Warning;
import fr.shom.ping.model.WarningSubscription;
import fr.shom.ping.model.dto.NotificationDto;
import fr.shom.ping.model.dto.SseNotificationDto;
import fr.shom.ping.model.enums.GeoregulationStatus;
import fr.shom.ping.model.enums.NotificationType;
import fr.shom.ping.repository.EmitterRepository;
import fr.shom.ping.repository.InfosourceRepository;
import fr.shom.ping.repository.SubscriptionRepository;
import fr.shom.ping.repository.SurveyRepository;
import fr.shom.ping.utils.GeoregulationUtil;
import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static java.util.stream.Collectors.toSet;

@Service
@AllArgsConstructor
@Slf4j
public class SseNotificationService {

    private final EmitterRepository emitterRepository;

    private final NotificationAdminService notificationAdminService;

    private final NotificationSecuredService notificationSecuredService;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private final SubscriptionRepository subscriptionRepository;

    private final EmailNotificationService emailNotificationService;

    private final InfosourceRepository infosourceRepository;

    private final KafkaService kafkaService;

    private final SurveyRepository surveyRepository;

    public void sendSubscriptionNotificationPublicUser(String title, NotificationType type, PublicUser publicUser, Subscription subscription) {
        Notification notification = notificationSecuredService.create(title, type, publicUser, subscription, null);
        sendNotification(publicUser.getId().toString(), new NotificationDto(notification));
    }

    public void sendSubscriptionNotificationService(String title, NotificationType type, MaritimeService service, Subscription subscription) {
        List<Notification> notifications = notificationAdminService.createForServiceUsersWithRoleOnService(title, type, service, subscription, null, null);
        for (Notification notification : notifications) {
            sendNotification(notification.getServiceUser().getId().toString(), new NotificationDto(notification));
        }
    }

    public void sendFolderNotificationService(String title, MaritimeService service, Folder folder) {
        sendFolderNotificationService(title, NotificationType.FOLDER, service, folder);
    }

    public void sendFolderNotificationService(String title, NotificationType type, MaritimeService service, Folder folder) {
        List<Notification> notifications = notificationAdminService
            .createForServiceUsersWithRoleOnService(title, type, service, null, folder, null);
        for (Notification notification : notifications) {
            sendNotification(notification.getServiceUser().getId().toString(), new NotificationDto(notification));
        }
    }

    public void sendLighthouseAndBeaconPreviousNoticeNumberNotifications(String title, MaritimeService service, Warning warning) {
        List<Notification> notifications = notificationAdminService
            .createForServiceUsersWithRoleOnService(title, service, warning);
        for (Notification notification : notifications) {
            sendNotification(notification.getServiceUser().getId().toString(), new NotificationDto(notification));
        }
    }

    public void sendGeoregulationInfosourceUpdateNotification(String title, Georegulation georegulation) {
        if (georegulation.getInfosource() == null) {
            return;
        }
        final List<Notification> notifications = notificationAdminService.createForServiceUsersLinkedToInfosource(title, georegulation);
        for (Notification notification : notifications) {
            sendNotification(notification.getServiceUser().getId().toString(), new NotificationDto(notification));
        }
    }

    @Async
    public void sendWarningNotification(Warning warning) {
        Set<WarningSubscription> warningSubscriptions = findAllSubscriptionsToNotifyWarning(warning.getId());
        NotificationType type = NotificationType.WARNING_PUBLICATION;
        for (WarningSubscription warningSubscription : warningSubscriptions.stream()
            .sorted(Comparator.comparing(Subscription::getCreationDate)).toList()) {
            String title = warningSubscription.getTitle() + " : " + warning.getNameOfSeries() + " " +
                (warning.getWarningNumber() != null ? warning.getWarningNumber() + "/" + warning.getYear().toString().substring(2) : "");
            if (warningSubscription.getService() != null) {
                List<Notification> notifications = notificationAdminService.createForServiceUsersWithRoleOnSubscriptionService(
                    title, type, warningSubscription, warning, null
                );
                for (Notification notification : notifications) {
                    final ServiceUser user = notification.getServiceUser();
                    sendNotification(user.getId().toString(), new NotificationDto(notification));
                    if (user.hasSubscriptionEmailNotifications()) {
                        emailNotificationService.sendNotification(warningSubscription, warning, user.getEmail());
                    }
                }
            } else {
                Notification notification = notificationSecuredService.create(title, type, warningSubscription.getPublicUser(), warningSubscription, warning);
                final PublicUser user = notification.getPublicUser();
                sendNotification(user.getId().toString(), new NotificationDto(notification));
                if (user.hasSubscriptionEmailNotifications()) {
                    emailNotificationService.sendNotification(warningSubscription, warning, user.getEmail());
                }
            }
        }
    }

    @Async
    public void sendGeoregulationNotification(Georegulation georegulation, NotificationType type) {
        if (georegulation.getStatus() != GeoregulationStatus.PUBLISHED && type == NotificationType.GEOREGULATION_UPDATED) {
            return;
        }
        final Set<GeoregulationSubscription> georegulationSubscriptions = findAllSubscriptionsToNotifyGeoregulation(georegulation.getId());

        final String number = GeoregulationUtil.getNumber(
            georegulation.getNumber().getYear(),
            georegulation.getNumber().getText(),
            georegulation.getNumber().getAuthorityCodes(),
            georegulation.getNumber().getDate()
        );

        for (GeoregulationSubscription georegulationSubscription : georegulationSubscriptions.stream()
            .sorted(Comparator.comparing(Subscription::getCreationDate)).collect(Collectors.toList())) {
            final String title = georegulationSubscription.getTitle() + " : " + number;
            final List<Notification> notifications = notificationAdminService.createForServiceUsersWithRoleOnSubscriptionService(
                title, type, georegulationSubscription, null, georegulation
            );
            for (Notification notification : notifications) {
                sendNotification(notification.getServiceUser().getId().toString(), new NotificationDto(notification));
                final ServiceUser user = notification.getServiceUser();
                if (user.hasSubscriptionEmailNotifications()) {
                    emailNotificationService.sendNotification(georegulationSubscription, georegulation, user.getEmail(), type);
                }
            }
        }

        // send notification to each recipient of IS of type GR when GR is PUBLISHED and SENT
        if (georegulation.getStatus() == GeoregulationStatus.PUBLISHED && infosourceRepository.existsByGeoregulationId(georegulation.getId())) {
            sendGeoregulationInfosourceUpdateNotification(number, georegulation);
        }
    }

    public void sendInfosourceNotification(String title, MaritimeService service, Infosource infosource) {
        final NotificationType type = NotificationType.INFOSOURCE_RECEIVED;
        final List<Notification> notifications = notificationAdminService.createForServiceUsersWithRoleOnService(title, type, service, null, null, infosource);
        for (Notification notification : notifications) {
            final ServiceUser user = notification.getServiceUser();
            sendNotification(user.getId().toString(), new NotificationDto(notification));
            emailNotificationService.sendInfosourceReceivedNotification(infosource, user.getEmail());
        }
    }

    public void sendSurveyNotification(Survey survey, NotificationType type) {
        final Survey surveyFromDatabase = surveyRepository.findById(survey.getId()).orElseThrow(NotFoundException::new);
        final List<Notification> notifications = notificationAdminService.createSurveyNotificationsForUsers(type, surveyFromDatabase);
        for (Notification notification : notifications) {
            sendNotification(notification.getUserId().toString(), new NotificationDto(notification));
        }

    }

    private void sendNotification(String userId, NotificationDto notification) {
        kafkaService.sendSseNotification(SseNotificationDto.builder().userId(userId).notificationDto(notification).build());
    }

    private void sendNotificationTask(String userId, NotificationDto notification) {
        emitterRepository.get(userId).ifPresent(sseEmitters -> {
            final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>(sseEmitters);
            final Iterator<SseEmitter> iterator = emitters.iterator();
            iterator.forEachRemaining(sseEmitter -> {
                try {
                    sseEmitter.send(SseEmitter.event().data(notification));
                } catch (IOException | IllegalStateException e) {
                    sseEmitter.completeWithError(e);
                    emitterRepository.remove(userId, sseEmitter);
                }
            });
        });
    }

    /**
     * Maintain SSE connection alive
     */
    @Scheduled(fixedRateString = "${ping.notifications.connection.heartbeat}")
    public void heartbeat() {
        try (ExecutorService sseExecutor = Executors.newCachedThreadPool()) {
            sseExecutor.execute(() -> emitterRepository.getAll().forEach((userId, sseEmitters) -> {
                final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>(sseEmitters);
                final Iterator<SseEmitter> iterator = emitters.iterator();
                iterator.forEachRemaining(sseEmitter -> {
                    try {
                        sseEmitter.send(SseEmitter.event().data("heartbeat"));
                    } catch (ClientAbortException e) {
                        log.info("Client {} stopped the connection", userId);
                    } catch (IOException | IllegalStateException e) {
                        sseEmitter.completeWithError(e);
                        emitterRepository.remove(userId, sseEmitter);
                    }
                    log.debug("heartbeat sent to all " + userId);
                });
            }));
        }
    }

    public Set<WarningSubscription> findAllSubscriptionsToNotifyWarning(UUID warningId) {
        final MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("warningId", warningId);

        List<UUID> subscriptionIds = this.namedParameterJdbcTemplate.queryForList(
            "select distinct s.id as id " +
                "from subscription s " +
                "        left join subscription_sub_area ssa on s.id = ssa.subscription_id " +
                "        left join (subscription_maritime_entity_geographic_extent smege " +
                "            left join maritime_entity me on smege.maritime_entity_id = me.id) on s.id = smege.subscription_id, " +
                "    warning w " +
                "        left join warning_general_area wga on w.id = wga.warning_id " +
                "        left join (warning_feature_part wfp " +
                "            left join warning_feature_part_geometry wfpg on wfp.id = wfpg.feature_part_id) on w.id = wfp.warning_id " +
                "where w.id = :warningId " +
                "and (w.warning_hazard_type_general in " +
                "    (select swhtg.warning_hazard_type_general " +
                "     from subscription_warning_hazard_type_general swhtg " +
                "     where s.id = swhtg.subscription_id) " +
                "         or (select count(*) " +
                "             from subscription_warning_hazard_type_general swhtg " +
                "             where s.id = swhtg.subscription_id) = 0) " +
                "and s.serie_id = w.serie_id " +
                "and (ssa.sub_area_id = wga.sub_area_id " +
                "         or (select count(*) " +
                "             from subscription_sub_area ssa2 " +
                "             where s.id = ssa2.subscription_id) = 0) " +
                "and (st_intersects(me.geom, wfpg.geom) or smege.subscription_id is null)",
            parameters,
            UUID.class
        );

        return subscriptionRepository.findAllById(subscriptionIds).stream().map(subscription -> (WarningSubscription) subscription).collect(toSet());
    }

    public Set<GeoregulationSubscription> findAllSubscriptionsToNotifyGeoregulation(UUID georegulationId) {
        // find all subscriptions with maritime service and GR types
        final MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("georegulationId", georegulationId);

        List<UUID> subscriptionIds = this.namedParameterJdbcTemplate.queryForList(
            "select distinct s.id as id " +
                "from subscription s " +
                "join subscription_georegulation_type sgt on sgt.subscription_id = s.id " +
                "join georegulation g on g.type = sgt.georegulation_type " +
                "join georegulation_number_authority gna on gna.georegulation_id = g.id " +
                "join maritime_service_areas msa on msa.areas_id = s.area_id and msa.maritime_service_id = gna.authority_id " +
                "left join georegulation_entity ge on ge.georegulation_id = g.id " +
                "left join (georegulation_part gp " +
                "left join georegulation_part_feature gpf on gp.id = gpf.part_id) on g.id = gp.georegulation_id " +
                "left join (subscription_maritime_entity_geographic_extent smege " +
                "left join maritime_entity me on smege.maritime_entity_id = me.id) on s.id = smege.subscription_id " +
                "left join subscription_maritime_entity sme on sme.maritime_entity_id = ge.entity_id " +
                "where g.id = :georegulationId " +
                "and s.type = 'GEOREGULATION' " +
                "and (st_intersects(me.geom, gpf.geom) or smege.subscription_id is null)",
            parameters,
            UUID.class
        );

        return subscriptionRepository.findAllById(subscriptionIds).stream().map(subscription -> (GeoregulationSubscription) subscription).collect(toSet());
    }

    public void sendEvent(SseNotificationDto sseNotificationDto) {
        try (ExecutorService sseExecutor = Executors.newCachedThreadPool()) {
            sseExecutor.execute(() -> sendNotificationTask(sseNotificationDto.getUserId(), sseNotificationDto.getNotificationDto()));
        }
    }
}
