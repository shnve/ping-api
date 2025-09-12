package fr.shom.ping.service;

import fr.shom.ping.exception.ForbiddenException;
import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.model.GeoregulationSubscription;
import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.ServiceUserRole;
import fr.shom.ping.model.Subscription;
import fr.shom.ping.model.WarningSubscription;
import fr.shom.ping.model.dto.SubscriptionCreateDto;
import fr.shom.ping.model.dto.SubscriptionDto;
import fr.shom.ping.model.dto.SubscriptionTableDto;
import fr.shom.ping.model.enums.NotificationType;
import fr.shom.ping.model.enums.SubscriptionType;
import fr.shom.ping.model.predicate.SubscriptionSearchCriteria;
import fr.shom.ping.repository.ServiceUserRepository;
import fr.shom.ping.repository.SubscriptionRepository;
import fr.shom.ping.utils.search.SubscriptionTableUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class SubscriptionAdminService {

    protected final EntityManager entityManager;

    private final SubscriptionRepository repository;

    private final ServiceUserRepository serviceUserRepository;

    private final MaritimeServiceService maritimeServiceService;

    private final AreaService areaService;

    private final SerieService serieService;

    private final SubAreaService subAreaService;

    private final MaritimeEntityService maritimeEntityService;

    private final SseNotificationService sseNotificationService;

    public PageImpl<SubscriptionTableDto> findAllBySearchCriteria(SubscriptionSearchCriteria searchCriteria, Pageable pageable, String username) {
        ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);
        return SubscriptionTableUtil.find(entityManager, searchCriteria, pageable, serviceUser);
    }

    public UUID createWarningSubscription(SubscriptionCreateDto dto, String username) {
        return createSubscription(dto, SubscriptionType.WARNING, username);
    }

    public UUID createGeoregulationSubscription(SubscriptionCreateDto dto, String username) {
        return createSubscription(dto, SubscriptionType.GEOREGULATION, username);
    }

    private UUID createSubscription(SubscriptionCreateDto dto, SubscriptionType type, String username) {
        ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);
        MaritimeService subscriptionService = maritimeServiceService.findById(dto.getServiceId());
        if (!serviceUser.getRoles().stream().map(ServiceUserRole::getServiceId).collect(Collectors.toList()).contains(subscriptionService.getId())) {
            throw new ForbiddenException();
        }

        Subscription subscription;
        if (type == SubscriptionType.WARNING) {
            subscription = new WarningSubscription();
            WarningSubscription warningSubscription = (WarningSubscription) subscription;
            warningSubscription.setType(SubscriptionType.WARNING);
            warningSubscription.setSerie(serieService.findById(dto.getSerieId()));
            warningSubscription.setLang(dto.getLang());
            warningSubscription.setWarningHazardTypeGenerals(dto.getWarningHazardTypeGenerals());
            warningSubscription.setSubAreas(new HashSet<>(subAreaService.findAllById(new ArrayList<>(dto.getSubAreaIds()))));
        } else {
            subscription = new GeoregulationSubscription();
            GeoregulationSubscription georegulationSubscription = (GeoregulationSubscription) subscription;
            georegulationSubscription.setType(SubscriptionType.GEOREGULATION);
            georegulationSubscription.setGeoregulationTypes(dto.getGeoregulationTypes());
            georegulationSubscription.setMaritimeEntities(dto.getMaritimeEntityIds().stream()
                .map(maritimeEntityService::findById).collect(Collectors.toSet()));
        }

        subscription.setTitle(dto.getTitle());
        subscription.setSendingMail(dto.isSendingMail());
        subscription.setService(subscriptionService);
        subscription.setMaritimeEntitiesGeographicExtent(dto.getMaritimeEntityIdsGeographicExtent().stream()
            .map(maritimeEntityService::findById).collect(Collectors.toSet()));
        subscription.setArea(areaService.getOne(dto.getAreaId()));

        UUID subscriptionId =  repository.save(subscription).getId();

        NotificationType notificationType = subscription.getType() == SubscriptionType.WARNING ?
            NotificationType.WARNING_SUBSCRIPTION_CREATION :
            NotificationType.GEOREGULATION_SUBSCRIPTION_CREATION;
        sseNotificationService.sendSubscriptionNotificationService(subscription.getTitle(), notificationType, subscription.getService(), subscription);

        return subscriptionId;
    }

    @Transactional(readOnly = true)
    public SubscriptionDto findById(UUID id, String username) {
        Subscription subscription = checkIfServiceUserHasRightsOnSubscription(id, username);
        return SubscriptionDto.fromSubscription(subscription);
    }

    @Transactional
    public void delete(UUID id, String username) {
        Subscription subscription = checkIfServiceUserHasRightsOnSubscription(id, username);
        repository.delete(subscription);
    }

    private Subscription checkIfServiceUserHasRightsOnSubscription(UUID id, String username) {
        ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);
        Subscription subscription = repository.findById(id).orElseThrow(NotFoundException::new);

        if(!serviceUser.getRoles().stream().map(ServiceUserRole::getServiceId).collect(Collectors.toList()).contains(subscription.getService().getId())) {
            throw new ForbiddenException();
        }

        return subscription;
    }
}
