package fr.shom.ping.service;

import fr.shom.ping.exception.ForbiddenException;
import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.model.Folder;
import fr.shom.ping.model.Georegulation;
import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.InfosourceRecipient;
import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.Notification;
import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.Subscription;
import fr.shom.ping.model.Survey;
import fr.shom.ping.model.Warning;
import fr.shom.ping.model.dto.NotificationDto;
import fr.shom.ping.model.enums.NotificationType;
import fr.shom.ping.repository.NotificationRepository;
import fr.shom.ping.repository.ServiceUserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

import static fr.shom.ping.utils.NotificationUtil.mapToNotificationDtoList;
import static fr.shom.ping.utils.SchemaUtil.NOTIFICATION_TABLE_TUPLE_MAPPING;

@Service
public class NotificationAdminService extends AbstractNotificationService {
    private final ServiceUserRepository serviceUserRepository;

    public NotificationAdminService(NotificationRepository repository, EntityManager entityManager, ServiceUserRepository serviceUserRepository) {
        super(repository, entityManager);
        this.serviceUserRepository = serviceUserRepository;
    }

    @Transactional
    public List<Notification> createForServiceUsersLinkedToInfosource(String title, Georegulation georegulation) {
        final Infosource infosource = georegulation.getInfosource();
        if (infosource == null) {
            return Collections.emptyList();
        }
        final Set<ServiceUser> serviceUsers = serviceUserRepository.findAllWithRoleOnServices(
            infosource.getRecipients().stream()
                .map(InfosourceRecipient::getService)
                .map(MaritimeService::getId)
                .collect(Collectors.toList()));
        final List<Notification> notifications = new ArrayList<>();
        for (ServiceUser serviceUser : serviceUsers) {
            notifications.add(Notification.builder()
                .title(title)
                .type(NotificationType.GEOREGULATION_UPDATED)
                .serviceUser(serviceUser)
                .georegulation(georegulation)
                .service(serviceUser.getService())
                .build());
        }
        return repository.saveAll(notifications);
    }

    @Transactional
    public List<Notification> createForServiceUsersWithRoleOnService(String title, MaritimeService service, Warning warning) {
        final List<ServiceUser> serviceUsers = serviceUserRepository.findAllWithRoleOnService(service.getId());
        final List<Notification> notifications = new ArrayList<>();
        for (ServiceUser serviceUser : serviceUsers) {
            notifications.add(Notification.builder()
                .title(title)
                .type(NotificationType.LIGHTHOUSE_AND_BEACON_PREVIOUS_NOTICE_NUMBER)
                .serviceUser(serviceUser)
                .warning(warning)
                .service(serviceUser.getService())
                .build());
        }
        return repository.saveAll(notifications);
    }

    @Transactional
    public List<Notification> createForServiceUsersWithRoleOnService(String title, NotificationType type, MaritimeService service, Subscription subscription,
        Folder folder, Infosource infosource) {
        List<ServiceUser> serviceUsers = serviceUserRepository.findAllWithRoleOnService(service.getId());
        List<Notification> notifications = new ArrayList<>();
        for (ServiceUser serviceUser : serviceUsers) {
            notifications.add(Notification.builder()
                .title(title)
                .type(type)
                .serviceUser(serviceUser)
                .subscription(subscription)
                .folder(folder)
                .service(service)
                .infosource(infosource)
                .build());
        }
        return repository.saveAll(notifications);
    }

    @Transactional
    public List<Notification> createForServiceUsersWithRoleOnSubscriptionService(String title, NotificationType type, Subscription subscription,
        Warning warning, Georegulation georegulation) {
        List<ServiceUser> serviceUsers = serviceUserRepository.findAllWithRoleOnService(subscription.getService().getId());
        List<Notification> notifications = new ArrayList<>();
        for (ServiceUser serviceUser : serviceUsers) {
            notifications.add(Notification.builder()
                .title(title)
                .type(type)
                .serviceUser(serviceUser)
                .subscription(subscription)
                .warning(warning)
                .georegulation(georegulation)
                .service(subscription.getService())
                .build());
        }
        return repository.saveAll(notifications);
    }

    @Transactional
    public List<Notification> createSurveyNotificationsForUsers(NotificationType type, Survey survey) {
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.systemDefault());
        final MaritimeService issuingService = survey.getIssuingService();
        final List<Notification> notifications = new ArrayList<>();
        final Notification notificationToEmitter = Notification.builder()
            .title(
                survey.getNameAndLocation() +
                    (issuingService != null ? " (" + issuingService.getName() + ")" : "") +
                    " - " + formatter.format(Instant.now())
            )
            .type(type)
            .publicUser(survey.getIssuingPublicUser())
            .serviceUser(survey.getIssuingServiceUser())
            .survey(survey)
            .read(false)
            .readDate(null)
            .creationDate(Instant.now())
            .build();
        notifications.add(notificationToEmitter);

        // if issuingService != null, then issuingServiceUser is already notified so we need to remove him from the list
        // and we have to notify other users who have rights on issuingService
        if (issuingService != null) {
            final List<ServiceUser> serviceUsers = serviceUserRepository.findAllWithRoleOnService(issuingService.getId()).stream()
                .filter(serviceUser -> !serviceUser.getId().equals(survey.getIssuingServiceUser().getId())).collect(Collectors.toList());
            for (ServiceUser serviceUser : serviceUsers) {
                notifications.add(Notification.builder()
                    .title(
                        survey.getNameAndLocation() + " (" + issuingService.getName() + ")" + " - " + formatter.format(Instant.now())
                    )
                    .type(type)
                    .serviceUser(serviceUser)
                    .survey(survey)
                    .read(false)
                    .readDate(null)
                    .creationDate(Instant.now())
                    .build());
            }
        }

        return repository.saveAll(notifications);
    }

    public List<NotificationDto> findAllByServiceUser(String username) {
        final ServiceUser user = getServiceUser(username);
        final Query query = entityManager.createNativeQuery(
            "select n.id, n.creation_date, n.title, n.type, n.read, n.read_date, n.folder_id, n.georegulation_id, " +
                "n.infosource_id, n.subscription_id, n.survey_id, n.warning_id, n.service_id, ms.name as service_name, " +
                "ms.international_name as service_international_name " +
                "from notification n " +
                "    left outer join maritime_service ms on n.service_id = ms.id " +
                "    left outer join folder f on n.folder_id = f.id " +
                "    left outer join georegulation g on n.georegulation_id = g.id " +
                "    left outer join infosource i on n.infosource_id = i.id " +
                "    left outer join subscription sub on n.subscription_id = sub.id " +
                "    left outer join survey s on n.survey_id = s.id " +
                "    left outer join warning w on n.warning_id = w.id " +
                "where n.service_user_id = :serviceUserId " +
                "order by n.creation_date desc;",
            NOTIFICATION_TABLE_TUPLE_MAPPING
        );

        query.setParameter("serviceUserId", user.getId());
        return mapToNotificationDtoList(query.getResultList());
    }

    protected List<Notification> getNotificationsIfBelongToUser(Set<UUID> ids, String username) {
        final ServiceUser user = getServiceUser(username);
        final List<Notification> notifications = repository.findAllById(ids);
        if (notifications.isEmpty()) {
            throw new NotFoundException();
        }
        notifications.forEach(notification -> {
            if (notification.getServiceUser().getId() != user.getId()) {
                throw new ForbiddenException();
            }
        });
        return notifications;
    }

    private ServiceUser getServiceUser(String username) {
        return serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);
    }
}
