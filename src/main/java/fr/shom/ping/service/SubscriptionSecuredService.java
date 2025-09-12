package fr.shom.ping.service;

import fr.shom.ping.exception.ForbiddenException;
import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.model.PublicUser;
import fr.shom.ping.model.Subscription;
import fr.shom.ping.model.WarningSubscription;
import fr.shom.ping.model.dto.SubscriptionCreateDto;
import fr.shom.ping.model.dto.SubscriptionDto;
import fr.shom.ping.model.dto.SubscriptionTableDto;
import fr.shom.ping.model.enums.NotificationType;
import fr.shom.ping.model.enums.SubscriptionType;
import fr.shom.ping.model.predicate.SubscriptionSearchCriteria;
import fr.shom.ping.repository.SubscriptionRepository;
import fr.shom.ping.repository.UserRepository;
import fr.shom.ping.utils.search.SubscriptionTableUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class SubscriptionSecuredService {

    protected final EntityManager entityManager;

    private final SubscriptionRepository repository;

    private final AreaService areaService;

    private final SerieService serieService;

    private final SubAreaService subAreaService;

    private final UserRepository userRepository;

    private final SseNotificationService sseNotificationService;

    public PageImpl<SubscriptionTableDto> findAllBySearchCriteria(SubscriptionSearchCriteria searchCriteria, Pageable pageable, String username) {
        PublicUser publicUser = userRepository.findOneByEmail(username);
        if (publicUser == null) {
            throw new ForbiddenException();
        }
        return SubscriptionTableUtil.find(entityManager, searchCriteria, pageable, publicUser);
    }

    public UUID createWarningSubscription(SubscriptionCreateDto dto, String username) {
        PublicUser publicUser = userRepository.findOneByEmail(username);
        if (publicUser == null) {
            throw new ForbiddenException();
        }
        WarningSubscription warningSubscription = new WarningSubscription();
        warningSubscription.setType(SubscriptionType.WARNING);
        warningSubscription.setTitle(dto.getTitle());
        warningSubscription.setSendingMail(dto.isSendingMail());
        warningSubscription.setPublicUser(publicUser);
        warningSubscription.setArea(areaService.getOne(dto.getAreaId()));
        warningSubscription.setSerie(serieService.findById(dto.getSerieId()));
        warningSubscription.setLang(dto.getLang());
        warningSubscription.setWarningHazardTypeGenerals(dto.getWarningHazardTypeGenerals());
        warningSubscription.setSubAreas(new HashSet<>(subAreaService.findAllById(new ArrayList<>(dto.getSubAreaIds()))));

        UUID subscriptionId =  repository.save(warningSubscription).getId();

        sseNotificationService.sendSubscriptionNotificationPublicUser(
            warningSubscription.getTitle(),
            NotificationType.WARNING_SUBSCRIPTION_CREATION,
            publicUser,
            warningSubscription
        );

        return subscriptionId;
    }

    @Transactional(readOnly = true)
    public SubscriptionDto findById(UUID id, String username) {
        Subscription subscription = checkIfPublicUserHasRightsOnSubscription(id, username);
        return SubscriptionDto.fromSubscription(subscription);
    }

    @Transactional
    public void delete(UUID id, String username) {
        Subscription subscription = checkIfPublicUserHasRightsOnSubscription(id, username);
        repository.delete(subscription);
    }

    private Subscription checkIfPublicUserHasRightsOnSubscription(UUID id, String username) {
        PublicUser publicUser = userRepository.findOneByEmail(username);
        if (publicUser == null) {
            throw new ForbiddenException();
        }
        Subscription subscription = repository.findById(id).orElseThrow(NotFoundException::new);

        if (subscription.getType() != SubscriptionType.WARNING || ((WarningSubscription) subscription).getPublicUser().getId() != publicUser.getId()) {
            throw new ForbiddenException();
        }

        return subscription;
    }
}
