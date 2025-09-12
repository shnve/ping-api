package fr.shom.ping.service;

import fr.shom.ping.exception.ForbiddenException;
import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.model.Notification;
import fr.shom.ping.model.PublicUser;
import fr.shom.ping.model.Subscription;
import fr.shom.ping.model.Warning;
import fr.shom.ping.model.dto.NotificationDto;
import fr.shom.ping.model.enums.NotificationType;
import fr.shom.ping.repository.NotificationRepository;
import fr.shom.ping.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

import static fr.shom.ping.utils.NotificationUtil.mapToNotificationDtoList;
import static fr.shom.ping.utils.SchemaUtil.NOTIFICATION_TABLE_TUPLE_MAPPING;

@Service
public class NotificationSecuredService extends AbstractNotificationService {
    private final UserRepository userRepository;

    public NotificationSecuredService(NotificationRepository repository, EntityManager entityManager, UserRepository userRepository) {
        super(repository, entityManager);
        this.userRepository = userRepository;
    }

    @Transactional
    public Notification create(String title, NotificationType type, PublicUser publicUser, Subscription subscription, Warning warning) {
        Notification notification = Notification.builder()
            .title(title)
            .type(type)
            .publicUser(publicUser)
            .subscription(subscription)
            .warning(warning)
            .build();
        return repository.save(notification);
    }

    public List<NotificationDto> findAllByPublicUser(String username) {
        final PublicUser user = getPublicUser(username);
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
                "where n.public_user_id = :publicUserId " +
                "order by n.creation_date desc;",
            NOTIFICATION_TABLE_TUPLE_MAPPING
        );

        query.setParameter("publicUserId", user.getId());
        return mapToNotificationDtoList(query.getResultList());
    }

    protected List<Notification> getNotificationsIfBelongToUser(Set<UUID> ids, String username) {
        final PublicUser user = getPublicUser(username);
        final List<Notification> notifications = repository.findAllById(ids);
        if (notifications.isEmpty()) {
            throw new NotFoundException();
        }
        notifications.forEach(notification -> {
            if (notification.getPublicUser().getId() != user.getId()) {
                throw new ForbiddenException();
            }
        });
        return notifications;
    }

    private PublicUser getPublicUser(String username) {
        final PublicUser publicUser = userRepository.findOneByEmail(username);
        if (publicUser == null) {
            throw new ForbiddenException();
        }
        return publicUser;
    }
}
