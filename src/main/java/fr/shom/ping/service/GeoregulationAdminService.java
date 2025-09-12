package fr.shom.ping.service;

import fr.shom.ping.exception.BadRequestException;
import fr.shom.ping.exception.ForbiddenException;
import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.Area;
import fr.shom.ping.model.Attachment;
import fr.shom.ping.model.EmailToSend;
import fr.shom.ping.model.Feature;
import fr.shom.ping.model.FixedDateRange;
import fr.shom.ping.model.Georegulation;
import fr.shom.ping.model.GeoregulationNumber;
import fr.shom.ping.model.GeoregulationPart;
import fr.shom.ping.model.MaritimeEntity;
import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.Serie;
import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.ServiceUserRole;
import fr.shom.ping.model.Warning;
import fr.shom.ping.model.dto.GeoregulationAdminTableDto;
import fr.shom.ping.model.dto.GeoregulationCreateDto;
import fr.shom.ping.model.dto.GeoregulationEditDto;
import fr.shom.ping.model.dto.GeoregulationModalDetailsAdminDto;
import fr.shom.ping.model.dto.GeoregulationModalDetailsDto;
import fr.shom.ping.model.dto.GeoregulationNumberDto;
import fr.shom.ping.model.dto.GeoregulationPartUpdateDto;
import fr.shom.ping.model.dto.GeoregulationSelectDto;
import fr.shom.ping.model.dto.GeoregulationSummaryUpdateDto;
import fr.shom.ping.model.dto.GeoregulationToDisplayOnCreationMapDto;
import fr.shom.ping.model.dto.GeoregulationUpdateDto;
import fr.shom.ping.model.dto.MaritimeEntityConcernedDto;
import fr.shom.ping.model.dto.MaritimeServiceInfosourceRecipientDto;
import fr.shom.ping.model.dto.MaritimeServiceSelectDto;
import fr.shom.ping.model.enums.GeoregulationStatus;
import fr.shom.ping.model.enums.GeoregulationType;
import fr.shom.ping.model.enums.NotificationType;
import fr.shom.ping.model.enums.ServiceRole;
import fr.shom.ping.model.predicate.GeoregulationAdminSearchCriteria;
import fr.shom.ping.properties.GeoregulationsCronCheckLinksProperties;
import fr.shom.ping.properties.ProxyProperties;
import fr.shom.ping.repository.GeoregulationRepository;
import fr.shom.ping.repository.InfosourceRepository;
import fr.shom.ping.repository.MaritimeEntityRepository;
import fr.shom.ping.repository.MaritimeServiceRepository;
import fr.shom.ping.repository.ServiceUserRepository;
import fr.shom.ping.repository.WarningRepository;
import fr.shom.ping.scheduling.SchedulerJobService;
import fr.shom.ping.utils.GeoregulationUtil;
import fr.shom.ping.utils.search.GeoregulationAdminTableUtil;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.locationtech.jts.geom.Geometry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newTreeSet;
import static fr.shom.ping.exception.ThrowingConsumerWrapper.throwingConsumerWrapper;
import static fr.shom.ping.model.enums.GeoregulationStatus.DRAFT;
import static fr.shom.ping.model.enums.GeoregulationStatus.PUBLISHED;
import static fr.shom.ping.model.enums.GeoregulationStatus.REPEALED;
import static fr.shom.ping.model.enums.MaritimeServiceType.AUTHORITY;
import static fr.shom.ping.model.enums.MaritimeServiceType.SHOM_GEOGRAPHIC_AREA;
import static fr.shom.ping.model.enums.ServiceRole.SERVICE_ADMIN;
import static fr.shom.ping.model.enums.ServiceRole.SERVICE_OPERATOR;
import static fr.shom.ping.utils.UserUtil.checkServiceUserHasNoRequiredRoleOnSerieService;
import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeoregulationAdminService {

    private final GeoregulationRepository repository;
    private final WarningRepository warningRepository;
    private final ServiceUserRepository serviceUserRepository;
    private final MaritimeServiceRepository maritimeServiceRepository;
    private final MaritimeEntityRepository maritimeEntityRepository;
    private final AttachmentService attachmentService;
    private final InfosourceRoutingService infosourceRoutingService;
    private final InfosourceAdminService infosourceAdminService;
    private final InfosourceRepository infosourceRepository;
    private final EntityManager entityManager;
    private final SchedulerJobService schedulerJobService;
    private final SerieService serieService;
    private final KafkaService kafkaService;
    private final GeoregulationsCronCheckLinksProperties georegulationsCronCheckLinksProperties;
    private final FolderService folderService;
    private final ProxyProperties proxyProperties;
    private final SseNotificationService sseNotificationService;


    @Transactional
    public UUID create(GeoregulationCreateDto dto, String username) {
        ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);

        if (serviceUser.getService().getType() == SHOM_GEOGRAPHIC_AREA) {
            throw new ForbiddenException();
        }

        MaritimeService issuingService = maritimeServiceRepository.findById(dto.getServiceId()).orElseThrow(BadRequestException::new);

        if (issuingService.getType() != AUTHORITY) {
            throw new ForbiddenException();
        }

        if (!isNumberAvailable(dto.getMainNumber())) {
            throw new BadRequestException("validation.georegulation.number.not-available");
        }

        List<MaritimeEntity> entities = isNotEmpty(dto.getMaritimeEntityIds()) ? maritimeEntityRepository.findAllById(dto.getMaritimeEntityIds()) :
            new ArrayList<>();

        if (isNotEmpty(dto.getMaritimeEntityIds()) && entities.size() != dto.getMaritimeEntityIds().size()) {
            throw new BadRequestException("At least one entity could not be found");
        }

        List<Attachment> attachments = attachmentService.findAllByIdIn(dto.getAttachmentIds());

        if (attachments.size() != dto.getAttachmentIds().size()) {
            throw new BadRequestException("At least one attachment could not be found");
        }

        Georegulation georegulation = dto.toGeoregulation(serviceUser, issuingService, entities, attachments);

        Set<UUID> mainAuthorityIds = newHashSet(dto.getMainNumber().getAuthorityIds());
        Set<UUID> additionalAuthorityIds = dto.getAdditionalNumbers().stream()
            .map(GeoregulationNumberDto::getAuthorityIds)
            .flatMap(Collection::stream)
            .collect(toSet());
        Set<UUID> authorityIds = Stream.of(mainAuthorityIds, additionalAuthorityIds)
            .flatMap(Collection::stream)
            .collect(toSet());

        List<UUID> areaIds = issuingService.getAreas().stream().map(Area::getId).collect(toList());
        List<MaritimeService> authorities = maritimeServiceRepository.findAuthoritiesByIdsAndAreaIds(newArrayList(authorityIds), areaIds);

        List<MaritimeService> mainAuthorities = authorities.stream()
            .filter(authority -> mainAuthorityIds.contains(authority.getId()))
            .collect(toList());

        if (mainAuthorities.size() != mainAuthorityIds.size()) {
            throw new BadRequestException("At least one authority from main number could not be found");
        }

        georegulation = georegulation.toBuilder()
            .number(GeoregulationNumber.builder()
                .year(dto.getMainNumber().getYear())
                .text(dto.getMainNumber().getText())
                .authorityCodes(mainAuthorities.stream().map(MaritimeService::getAuthorityCode).collect(toList()))
                .authorities(mainAuthorities)
                .date(dto.getMainNumber().getDate())
                .build())
            .build();

        List<GeoregulationNumberDto> additionalNumbers = dto.getAdditionalNumbers();
        if (isNotEmpty(additionalNumbers)) {
            List<MaritimeService> additionalAuthorities = authorities.stream()
                .filter(authority -> additionalAuthorityIds.contains(authority.getId()))
                .collect(toList());

            if (additionalAuthorities.size() != additionalAuthorityIds.size()) {
                throw new BadRequestException("At least one authority from additional numbers could not be found");
            }

            georegulation = georegulation.toBuilder()
                .additionalNumbers(additionalNumbers.stream().map(number -> {
                    List<MaritimeService> maritimeServices = additionalAuthorities.stream()
                        .filter(service -> number.getAuthorityIds().contains(service.getId())).collect(toList());

                    return GeoregulationNumber.builder()
                        .year(number.getYear())
                        .text(number.getText())
                        .authorityCodes(maritimeServices.stream().map(MaritimeService::getAuthorityCode).collect(toList()))
                        .authorityIds(maritimeServices.stream().map(MaritimeService::getId).collect(toList()))
                        .date(number.getDate())
                        .build();
                }).collect(toList()))
                .build();
        }

        final Georegulation savedGeoregulation = repository.save(georegulation);
        return savedGeoregulation.getId();
    }

    public List<GeoregulationToDisplayOnCreationMapDto> findAllToDisplayOnCreationMap(GeoregulationType georegulationType, UUID issuingServiceId) {
        return GeoregulationUtil.groupGeoregulationToDisplayOnCreationMapTupleByGeoregulationId(
            repository.findAllToDisplayOnCreationMapByServiceAndType(issuingServiceId, georegulationType),
            georegulationType
        );
    }

    public Page<GeoregulationAdminTableDto> findAllBySearchCriteria(GeoregulationAdminSearchCriteria searchCriteria, Pageable pageable) {
        return GeoregulationAdminTableUtil.find(entityManager, searchCriteria, pageable);
    }

    public List<MaritimeServiceSelectDto> getAuthoritiesToSelect(String username) {
        return maritimeServiceRepository.findAuthoritiesByUsername(username);
    }

    public List<MaritimeServiceSelectDto> getAuthoritiesWithAreasInIssuingServiceAreas(UUID issuingServiceId) {
        return maritimeServiceRepository.findAuthoritiesWithAreasInIssuingServiceAreas(issuingServiceId);
    }

    public List<MaritimeEntityConcernedDto> getAllGeoregulationEntities(String username) {
        final Optional<ServiceUser> serviceUser = serviceUserRepository.findByEmail(username);
        if (serviceUser.isEmpty()) {
            throw new ForbiddenException();
        }
        return Stream.concat(
                repository.getAllGeoregulationEntities().stream(),
                maritimeEntityRepository.findObservedEntities(serviceUser.get().getService().getId()).stream().map(MaritimeEntityConcernedDto::new)
            )
            .distinct()
            .collect(Collectors.toList());
    }

    public GeoregulationModalDetailsDto getModalDetails(UUID id, String username) {
        ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElse(null);
        Georegulation georegulation = repository.findById(id).orElseThrow(NotFoundException::new);

        boolean serviceUserHasRoleOnGeoregulationIssuingService = serviceUser != null && serviceUser.getRoles().stream()
            .map(ServiceUserRole::getServiceId).collect(toList()).contains(georegulation.getIssuingService().getId());

        boolean serviceUserHasRoleOnServiceFolderManager = serviceUser != null && maritimeServiceRepository.hasAnyFolderManagerByIds(
            serviceUser.getRoles().stream()
                .filter(serviceUserRole -> asList(SERVICE_OPERATOR, SERVICE_ADMIN).contains(serviceUserRole.getServiceRole()))
                .map(ServiceUserRole::getServiceId)
                .collect(toList())
        );

        boolean canBeAddedToFolder = georegulation.getStatus() != GeoregulationStatus.DRAFT && serviceUserHasRoleOnServiceFolderManager;

        if (serviceUserHasRoleOnGeoregulationIssuingService) {
            boolean hasAlreadyBeenSent = infosourceRepository.existsByGeoregulationId(id);

            return new GeoregulationModalDetailsAdminDto(georegulation, hasAlreadyBeenSent, canBeAddedToFolder);
        } else if (georegulation.getStatus() != DRAFT) {
            return new GeoregulationModalDetailsDto(georegulation, canBeAddedToFolder);
        } else {
            throw new ForbiddenException();
        }
    }

    public ServiceUser getServiceUser(String username) {
        return serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);
    }

    private Georegulation checkServiceUserRoleAndGetGeoregulation(ServiceUser serviceUser, UUID georegulationId) {
        List<UUID> serviceIds = serviceUser.getServiceIdsWithOperatorOrAdminRole();

        return repository.findByIdAndIssuingServiceIdIn(georegulationId, serviceIds).orElseThrow(NotFoundException::new);
    }

    @Transactional
    public void publish(UUID id, String username) {
        final ServiceUser serviceUser = getServiceUser(username);

        final Georegulation georegulation = checkServiceUserRoleAndGetGeoregulation(serviceUser, id);

        if (georegulation.getStatus() != DRAFT) {
            throw new BadRequestException("Current georegulation status is invalid");
        }

        final Instant now = now();
        georegulation.setLastUpdate(now);
        georegulation.setLastUpdateUser(serviceUser);
        georegulation.setPublicationDate(now);
        georegulation.setStatus(PUBLISHED);

        // schedule job to change 'new' status
        schedulerJobService.scheduleAutoUpdateGeoregulationNewStatusJob(georegulation);

        // schedule job to change 'expired' status
        schedulerJobService.scheduleAutoUpdateGeoregulationExpiredStatusJob(georegulation);

        repository.save(georegulation);

        sseNotificationService.sendGeoregulationNotification(georegulation, NotificationType.GEOREGULATION_PUBLISHED);
    }

    @Transactional
    public void repeal(UUID id, String username) {
        final ServiceUser serviceUser = getServiceUser(username);

        final Georegulation georegulation = checkServiceUserRoleAndGetGeoregulation(serviceUser, id);

        if (georegulation.getStatus() != PUBLISHED) {
            throw new BadRequestException("Current georegulation status is invalid");
        }

        final Instant now = now();
        georegulation.setLastUpdate(now);
        georegulation.setLastUpdateUser(serviceUser);
        georegulation.setRepealDate(now);
        georegulation.setStatus(REPEALED);

        repository.save(georegulation);

        sseNotificationService.sendGeoregulationNotification(georegulation, NotificationType.GEOREGULATION_REPEALED);
    }

    public List<MaritimeServiceInfosourceRecipientDto> getRecipients(UUID id, String username) {
        ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);
        Georegulation georegulation = repository.findById(id).orElseThrow(NotFoundException::new);

        return getRecipients(serviceUser, georegulation);
    }

    private List<MaritimeServiceInfosourceRecipientDto> getRecipients(ServiceUser serviceUser, Georegulation georegulation) {
        if (!asList(PUBLISHED, REPEALED).contains(georegulation.getStatus())) {
            throw new BadRequestException();
        }

        boolean serviceUserHasNoRoleOnGeoregulationIssuingService = serviceUser.getRoles().stream()
            .noneMatch(serviceUserRole -> serviceUserRole.getServiceId().equals(georegulation.getIssuingService().getId()));

        if (serviceUserHasNoRoleOnGeoregulationIssuingService) {
            throw new ForbiddenException();
        }

        UUID issuingServiceId = georegulation.getIssuingService().getId();
        List<Geometry> geometries = georegulation.getParts().stream()
            .map(GeoregulationPart::getFeatures)
            .flatMap(Collection::stream)
            .map(Feature::getGeometry)
            .collect(toList());
        List<FixedDateRange> dateRanges = Stream.of(
                georegulation.getCalendarPeriods(),
                georegulation.getParts().stream()
                    .map(GeoregulationPart::getCalendarPeriods)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet()))
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .collect(toList());
        LocalDate signingDate = georegulation.getNumber().getDate();

        try {
            return infosourceRoutingService.getRecipientsForGeoregulationByDetails(
                issuingServiceId,
                geometries,
                dateRanges,
                signingDate
            );
        } catch (ServiceException exception) {
            throw new BadRequestException(exception.getMessage());
        }
    }

    public void send(UUID id, Set<UUID> recipientIds, String username) {
        ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);
        Georegulation georegulation = repository.findById(id).orElseThrow(NotFoundException::new);
        List<MaritimeServiceInfosourceRecipientDto> recipients = getRecipients(serviceUser, georegulation);

        Set<UUID> expectedRecipientIds = recipients.stream().map(MaritimeServiceInfosourceRecipientDto::getId).collect(Collectors.toSet());
        if (!expectedRecipientIds.containsAll(recipientIds)) {
            throw new BadRequestException("Recipient IDs does not match expected recipient IDs");
        }

        infosourceAdminService.createAndSendGeoregulation(georegulation, recipientIds, serviceUser);
    }


    @Transactional
    public UUID update(GeoregulationUpdateDto dto, String username) {
        final ServiceUser serviceUser = getServiceUser(username);

        final Georegulation georegulation = checkServiceUserRoleNotShomGeographicAreaAndGetGeoregulation(serviceUser, dto.getId());

        if (georegulation.getStatus() == REPEALED) {
            throw new BadRequestException("Georegulation must be in DRAFT or PUBLISHED status");
        }

        boolean mainNumberHasBeenUpdated = !(dto.getMainNumber().getYear().equals(georegulation.getNumber().getYear()) &&
            dto.getMainNumber().getText().equals(georegulation.getNumber().getText()) &&
            dto.getMainNumber().getDate().equals(georegulation.getNumber().getDate()) &&
            dto.getMainNumber().getAuthorityIds().containsAll(georegulation.getNumber().getAuthorities().stream().map(MaritimeService::getId).collect(toList())));

        if (mainNumberHasBeenUpdated && !isNumberAvailable(dto.getMainNumber())) {
            throw new BadRequestException("validation.georegulation.number.not-available");
        }

        final MaritimeService issuingService = maritimeServiceRepository.findById(dto.getServiceId()).get();

        final List<UUID> mainAuthorityIds = dto.getMainNumber().getAuthorityIds();
        final Set<UUID> additionalAuthorityIds = dto.getAdditionalNumbers().stream()
            .map(GeoregulationNumberDto::getAuthorityIds)
            .flatMap(Collection::stream)
            .collect(toSet());
        final Set<UUID> authorityIds = Stream.of(mainAuthorityIds, additionalAuthorityIds)
            .flatMap(Collection::stream)
            .collect(toSet());

        final List<UUID> areaIds = issuingService.getAreas().stream().map(Area::getId).collect(toList());
        final List<MaritimeService> authorities = maritimeServiceRepository.findAuthoritiesByIdsAndAreaIds(new ArrayList<>(authorityIds), areaIds);

        final List<MaritimeService> mainAuthorities = authorities.stream()
            .filter(authority -> mainAuthorityIds.contains(authority.getId()))
            .collect(toList());

        if (mainAuthorities.size() != mainAuthorityIds.size()) {
            throw new BadRequestException("At least one authority from main number could not be found");
        }

        List<MaritimeEntity> entities = new ArrayList<>();
        if (isNotEmpty(dto.getMaritimeEntityIds())) {
            entities = maritimeEntityRepository.findAllById(dto.getMaritimeEntityIds());

            if (entities.size() != dto.getMaritimeEntityIds().size()) {
                throw new BadRequestException("At least one entity could not be found");
            }
        }

        final List<Attachment> attachments = attachmentService.findAllByIdIn(dto.getAttachmentIds());

        if (attachments.size() != dto.getAttachmentIds().size()) {
            throw new BadRequestException("At least one attachment could not be found");
        }

        final List<MaritimeService> additionalAuthorities = authorities.stream()
            .filter(authority -> additionalAuthorityIds.contains(authority.getId()))
            .collect(toList());

        if (additionalAuthorities.size() != additionalAuthorityIds.size()) {
            throw new BadRequestException("At least one authority from additional numbers could not be found");
        }

        georegulation.setIssuingService(issuingService);
        georegulation.setType(dto.getType());

        final BiFunction<GeoregulationNumberDto, List<MaritimeService>, GeoregulationNumber> fromAdditionalNumberDto =
            (georegulationNumberDto, authorityList) -> GeoregulationNumber.builder()
                .year(georegulationNumberDto.getYear())
                .text(georegulationNumberDto.getText())
                .authorityCodes(authorityList.stream().map(MaritimeService::getAuthorityCode).collect(toList()))
                .authorityIds(authorityList.stream().map(MaritimeService::getId).collect(toList()))
                .date(georegulationNumberDto.getDate())
                .build();

        georegulation.getNumber().setYear(dto.getMainNumber().getYear());
        georegulation.getNumber().setText(dto.getMainNumber().getText());
        georegulation.getNumber().setAuthorityCodes(mainAuthorities.stream().map(MaritimeService::getAuthorityCode).collect(toList()));
        georegulation.getNumber().setDate(dto.getMainNumber().getDate());

        if (georegulation.getNumber().getAuthorities() != null) {
            georegulation.getNumber().getAuthorities().clear();
        } else {
            georegulation.getNumber().setAuthorities(newArrayList());
        }

        georegulation.getNumber().getAuthorities().addAll(newHashSet(mainAuthorities));

        if (georegulation.getAdditionalNumbers() != null) {
            georegulation.getAdditionalNumbers().clear();
        } else {
            georegulation.setAdditionalNumbers(newArrayList());
        }
        final Set<GeoregulationNumber> newAdditionalNumbers = dto.getAdditionalNumbers().stream().map(
            georegulationNumberDto -> {
                final List<MaritimeService> maritimeServices = additionalAuthorities.stream()
                    .filter(service -> georegulationNumberDto.getAuthorityIds().contains(service.getId())).collect(toList());
                return fromAdditionalNumberDto.apply(georegulationNumberDto, maritimeServices);
            }
        ).collect(toSet());
        georegulation.getAdditionalNumbers().addAll(newAdditionalNumbers);

        georegulation.setActivities(dto.getActivities());
        georegulation.setTitle(dto.getTitle());
        georegulation.setInternationalTitle(dto.getInternationalTitle());

        georegulation.getEntities().clear();
        georegulation.getEntities().addAll(newHashSet(entities));

        final boolean wasTemporaryBeforeUpdate = georegulation.isTemporary() && !dto.getIsTemporary();

        georegulation.setSummary(dto.getSummary());
        georegulation.setInternationalSummary(dto.getInternationalSummary());
        georegulation.setTemporary(dto.getIsTemporary());
        georegulation.setAllTerritorialSea(dto.getIsAllTerritorialSea());
        georegulation.setAnnouncedByAvurnavAvirade(dto.getIsAnnouncedByAvurnavAvirade());


        if (dto.getIsTemporary()) {
            final AtomicInteger calendarPeriodPosition = new AtomicInteger(1);
            final SortedSet<FixedDateRange> calendarPeriods = dto.getCalendarPeriods().stream().map(
                calendarPeriod -> {
                    calendarPeriod.setPosition(calendarPeriodPosition.getAndIncrement());
                    return calendarPeriod;
                }
            ).collect(toCollection(TreeSet::new));
            georegulation.setCalendarPeriods(calendarPeriods);
        } else if (wasTemporaryBeforeUpdate) {
            if (!isEmpty(georegulation.getCalendarPeriods())) {
                georegulation.getCalendarPeriods().clear();
            }

            georegulation.getParts().forEach(part -> {
                if (!isEmpty(part.getCalendarPeriods())) {
                    part.getCalendarPeriods().clear();
                }
            });
        }

        AtomicInteger seasonalPeriodPosition = new AtomicInteger(1);
        georegulation.setSeasonalPeriods(
            dto.getSeasonalPeriods().stream()
                .peek(seasonalPeriod -> seasonalPeriod.setPosition(seasonalPeriodPosition.getAndIncrement()))
                .collect(toCollection(TreeSet::new))
        );

        georegulation.setHasCoastlineLimit(dto.isHasCoastlineLimit());
        georegulation.setLinks(dto.getLinks());

        // delete unused attachments
        deleteUnusedAttachments(georegulation.getAttachments(), dto.getAttachmentIds());
        georegulation.getAttachments().clear();
        georegulation.getAttachments().addAll(newHashSet(attachments));

        georegulation.setPublic(dto.getIsPublic());
        georegulation.setMarkAsNewInMonths(dto.getMarkAsNewInMonths());
        georegulation.setInternalComment(dto.getInternalComment());
        georegulation.setTimezone(dto.getTimezone());
        georegulation.setLastUpdate(Instant.now());
        georegulation.setLastUpdateUser(serviceUser);

        final AtomicInteger partPosition = new AtomicInteger(1);
        final SortedSet<GeoregulationPart> parts = dto.getParts().stream()
            .map(part -> part.toGeoregulationPart(partPosition.getAndIncrement(), dto.getIsTemporary()))
            .collect(toCollection(TreeSet::new));
        georegulation.getParts().clear();
        georegulation.getParts().addAll(parts);

        final Georegulation savedGeoregulation = entityManager.merge(georegulation);

        if (savedGeoregulation.getStatus() == PUBLISHED) {
            // schedule job to change 'new' status
            schedulerJobService.scheduleAutoUpdateGeoregulationNewStatusJob(savedGeoregulation);

            // schedule job to change 'expired' status
            if (savedGeoregulation.isTemporary()) {
                schedulerJobService.scheduleAutoUpdateGeoregulationExpiredStatusJob(savedGeoregulation);
            } else if (wasTemporaryBeforeUpdate) {
                schedulerJobService.removeScheduleAutoUpdateGeoregulationExpiredStatusJob(savedGeoregulation);
            }
        }

        sseNotificationService.sendGeoregulationNotification(savedGeoregulation, NotificationType.GEOREGULATION_UPDATED);

        return savedGeoregulation.getId();
    }

    private void deleteUnusedAttachments(Collection<Attachment> attachments, List<UUID> updatedAttachmentList) {
        final List<UUID> formerAttachmentList = attachments.stream().map(Attachment::getId).collect(toList());
        final List<UUID> deletedAttachments = findDifferences(formerAttachmentList, updatedAttachmentList);

        // delete files from disk using AttachmentService.delete()
        deletedAttachments.forEach(throwingConsumerWrapper(this.attachmentService::deleteFromDiskAndDatabase));
    }

    /**
     * Find differences between two lists of same type: find items from comparedList not in referenceList
     *
     * @param comparedList  the list we want to get difference
     * @param referenceList the list used as reference comparison
     * @return a list with items that are in comparedList but not in referenceList
     */
    private static <T> List<T> findDifferences(List<T> comparedList, List<T> referenceList) {
        return comparedList.stream().filter(e -> !referenceList.contains(e)).collect(toList());
    }

    public GeoregulationEditDto findById(UUID id, String username) {
        final ServiceUser serviceUser = getServiceUser(username);
        final Georegulation georegulation = checkServiceUserRoleOrShomGeographicAreaAndGetGeoregulation(serviceUser, id);
        return GeoregulationEditDto.from(georegulation);
    }

    public List<GeoregulationSelectDto> findAvailableToLinkToWarning(UUID serieId, String username) {
        ServiceUser serviceUser = getServiceUser(username);

        Serie serie = serieService.findById(serieId);
        MaritimeService service = serie.getMaritimeService();

        boolean serviceUserHasNoRequiredRoleOnSerieService = checkServiceUserHasNoRequiredRoleOnSerieService(
            serviceUser, service, ServiceRole.SERVICE_OPERATOR, ServiceRole.SERVICE_ADMIN
        );

        if (serviceUserHasNoRequiredRoleOnSerieService) {
            throw new ForbiddenException();
        }

        return repository.findByIssuingServiceOrRecipient(service).stream().map(GeoregulationSelectDto::new).collect(toList());
    }

    @Transactional
    public void updateSummary(UUID id, GeoregulationSummaryUpdateDto dto, String username) {
        final ServiceUser serviceUser = getServiceUser(username);
        final Georegulation georegulation = checkServiceUserRoleOrShomGeographicAreaAndGetGeoregulation(serviceUser, id);

        if (georegulation.getStatus() == REPEALED) {
            throw new BadRequestException("Georegulation must be in DRAFT or PUBLISHED status");
        }

        georegulation.setSummary(dto.getSummary());
        georegulation.setInternationalSummary(dto.getInternationalSummary());
        georegulation.setLastUpdate(Instant.now());
        georegulation.setLastUpdateUser(serviceUser);

        if (isNotEmpty(dto.getParts()) && georegulation.getParts().size() == dto.getParts().size()) {
            georegulation.getParts().forEach(part -> {
                part.getTypes().clear();
                part.getTypes().addAll(dto.getParts().get(part.getPosition() - 1).getTypes());
            });


            final AtomicInteger partPosition = new AtomicInteger(1);
            final SortedSet<GeoregulationPartUpdateDto> parts = dto.getParts().stream()
                .map(part -> {
                    part.setPosition(partPosition.getAndIncrement());
                    return part;
                })
                .collect(toCollection(TreeSet::new));

            georegulation.getParts().forEach(part -> {
                final GeoregulationPartUpdateDto found =
                    parts.stream().filter(georegulationPartUpdateDto -> georegulationPartUpdateDto.getPosition().equals(part.getPosition())).findFirst().get();
                part.getTypes().clear();
                part.getTypes().addAll(found.getTypes());
            });
        }

        repository.save(georegulation);

        sseNotificationService.sendGeoregulationNotification(georegulation, NotificationType.GEOREGULATION_UPDATED);
    }

    private Georegulation checkServiceUserRoleNotShomGeographicAreaAndGetGeoregulation(ServiceUser serviceUser, UUID georegulationId) {
        if (serviceUser.getService().getType() == SHOM_GEOGRAPHIC_AREA) {
            throw new ForbiddenException();
        }
        return checkServiceUserRoleAndGetGeoregulation(serviceUser, georegulationId);
    }

    private Georegulation checkServiceUserRoleOrShomGeographicAreaAndGetGeoregulation(ServiceUser serviceUser, UUID georegulationId) {
        Georegulation georegulation;
        try {
            georegulation = checkServiceUserRoleAndGetGeoregulation(serviceUser, georegulationId);
        } catch (NotFoundException e) {
            if (serviceUser.getService().getType() != SHOM_GEOGRAPHIC_AREA) {
                throw new ForbiddenException();
            }
            georegulation = repository.findById(georegulationId).orElseThrow(NotFoundException::new);
        }
        return georegulation;
    }

    @Transactional
    public void autoUpdateGeoregulationNewStatus(UUID georegulationId) {
        boolean hasBeenUpdated = repository.updateNewStatusToFalse(georegulationId) == 1;
        if (!hasBeenUpdated) {
            throw new BadRequestException();
        }
    }

    private boolean isNumberAvailable(GeoregulationNumberDto numberDto) {
        List<Georegulation> numberExceptAuthorities = repository.findByNumberExceptAuthorities(numberDto.getYear(), numberDto.getText(),
            numberDto.getDate());

        return numberExceptAuthorities.isEmpty() || numberExceptAuthorities.stream()
            .map(Georegulation::getNumber)
            .map(GeoregulationNumber::getAuthorities)
            .map(maritimeServices -> maritimeServices.stream().map(MaritimeService::getId).collect(toList()))
            .noneMatch(ids -> numberDto.getAuthorityIds().containsAll(ids));
    }

    @Transactional
    public void delete(UUID id, String username) {
        ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);
        Georegulation georegulation = repository.findById(id).orElseThrow(NotFoundException::new);

        boolean serviceUserHasNoRoleOnGeoregulationIssuingService = serviceUser.getRoles().stream()
            .noneMatch(serviceUserRole -> serviceUserRole.getServiceId().equals(georegulation.getIssuingService().getId()));

        if (serviceUserHasNoRoleOnGeoregulationIssuingService) {
            throw new ForbiddenException();
        }

        if (!asList(DRAFT, REPEALED).contains(georegulation.getStatus())) {
            throw new BadRequestException();
        }

        georegulation.getAttachments().forEach(attachment -> {
            try {
                attachmentService.deleteAttachmentFile(attachment);
            } catch (ServiceException ignored) {
                log.debug("Failed to delete attachment with id {}", attachment.getId());
            }
        });

        repository.delete(georegulation);
    }

    @Transactional
    @Scheduled(cron = "${ping.georegulations-cron-check-links.cron-value}")
    @SuppressWarnings("findsecbugs:URLCONNECTION_SSRF_FD")
    @SchedulerLock(
        name = "ping-scheduler_georegulation_autoCheckLinks",
        lockAtLeastFor = "${ping.shedlock.lock-time-at-least-for}",
        lockAtMostFor = "${ping.shedlock.lock-time-at-most-for}"
    )
    public void autoCheckLinks() {
        LockAssert.assertLocked();

        List<Georegulation> georegulations = repository.findAllToCheckLinks();
        List<String> listMessagesToSend = new ArrayList<>();
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyProperties.getProxyHost(), proxyProperties.getProxyPort()));

        for (Georegulation georegulation : georegulations) {
            List<String> invalidLinks = new ArrayList<>();
            for (String link : georegulation.getLinks()) {
                try {
                    URL url = new URL(link);
                    HttpURLConnection httpURLConnection;
                    if (proxyProperties.isUseProxy()) {
                        httpURLConnection = (HttpURLConnection) url.openConnection(proxy);
                    } else {
                        httpURLConnection = (HttpURLConnection) url.openConnection();
                    }
                    if (httpURLConnection.getResponseCode() >= 400) {
                        invalidLinks.add(link);
                    }
                } catch (IOException ignored) {
                    invalidLinks.add(link);
                }
            }
            if (invalidLinks.size() > 0) {
                StringBuilder message = new StringBuilder(GeoregulationUtil.getNumber(
                    georegulation.getNumber().getYear(),
                    georegulation.getNumber().getText(),
                    Set.copyOf(georegulation.getNumber().getAuthorityCodes()),
                    georegulation.getNumber().getDate()
                ) + " : \n");
                for (String link : invalidLinks) {
                    message.append(link).append("\n");
                }
                message.append("\n");
                listMessagesToSend.add(String.valueOf(message));
            }
        }

        if (listMessagesToSend.size() > 0) {
            kafkaService.sendInvalidGeoregulationsLinksMessage(
                EmailToSend.builder()
                    .recipients(georegulationsCronCheckLinksProperties.getEmailRecipient())
                    .subject(georegulationsCronCheckLinksProperties.getEmailSubject())
                    .body(georegulationsCronCheckLinksProperties.getEmailBodyStart() + "\n\n" + String.join("", listMessagesToSend))
                    .build()
            );
        }
    }

    @Transactional
    public void autoUpdateGeoregulationExpiredStatus(UUID georegulationId) {
        boolean hasBeenUpdated = repository.updateExpiredStatus(georegulationId, true) == 1;
        if (!hasBeenUpdated) {
            throw new BadRequestException();
        }
    }

    @Transactional
    public UUID clone(UUID id, String clonedGeoregulationNumberText, String username) {
        final ServiceUser serviceUser = getServiceUser(username);
        final Georegulation georegulation = checkServiceUserRoleAndGetGeoregulation(serviceUser, id);

        GeoregulationNumberDto clonedGeoregulationNumberDto = GeoregulationNumberDto.builder()
            .year(georegulation.getNumber().getYear())
            .text(clonedGeoregulationNumberText)
            .authorityIds(new ArrayList<>(georegulation.getNumber().getAuthorities().stream().map(MaritimeService::getId).collect(toList())))
            .date(georegulation.getNumber().getDate())
            .build();

        if (!isNumberAvailable(clonedGeoregulationNumberDto)) {
            throw new BadRequestException("validation.georegulation.number.not-available");
        }

        Georegulation clonedGeoregulation = Georegulation.builder()
            .issuingService(georegulation.getIssuingService())
            .type(georegulation.getType())
            .number(GeoregulationNumber.builder()
                .year(georegulation.getNumber().getYear())
                .text(clonedGeoregulationNumberText)
                .authorityCodes(new ArrayList<>(georegulation.getNumber().getAuthorityCodes()))
                .authorities(new ArrayList<>(georegulation.getNumber().getAuthorities()))
                .authorityIds(new ArrayList<>(georegulation.getNumber().getAuthorityIds()))
                .date(georegulation.getNumber().getDate())
                .build())
            .additionalNumbers(new ArrayList<>(georegulation.getAdditionalNumbers()))
            .activities(new ArrayList<>(georegulation.getActivities()))
            .title(georegulation.getTitle())
            .internationalTitle(georegulation.getInternationalTitle())
            .entities(new ArrayList<>(georegulation.getEntities()))
            .summary(georegulation.getSummary())
            .internalComment(georegulation.getInternalComment())
            .internationalSummary(georegulation.getInternationalSummary())
            .isTemporary(georegulation.isTemporary())
            .isAllTerritorialSea(georegulation.isAllTerritorialSea())
            .isAnnouncedByAvurnavAvirade(georegulation.isAnnouncedByAvurnavAvirade())
            .calendarPeriods(new TreeSet<>(georegulation.getCalendarPeriods()))
            .seasonalPeriods(new TreeSet<>(georegulation.getSeasonalPeriods()))
            .hasCoastlineLimit(georegulation.isHasCoastlineLimit())
            .links(new ArrayList<>(georegulation.getLinks()))
            .isPublic(georegulation.isPublic())
            .markAsNewInMonths(georegulation.getMarkAsNewInMonths())
            .timezone(georegulation.getTimezone())
            .lastUpdateUser(serviceUser)
            .isNew(true)
            .parts(new TreeSet<>(georegulation.getParts().stream().map(part -> GeoregulationPart.builder()
                .position(part.getPosition())
                .types(new ArrayList<>(part.getTypes()))
                .calendarPeriods(new TreeSet<>(part.getCalendarPeriods()))
                .seasonalPeriods(newTreeSet(part.getSeasonalPeriods()))
                .information(part.getInformation())
                .internationalInformation(part.getInternationalInformation())
                .isCommercialShip(part.getIsCommercialShip())
                .lengthVessel(part.getLengthVessel())
                .grossTonnageVessel(part.getGrossTonnageVessel())
                .choiceLengthVessel(part.getChoiceLengthVessel())
                .choiceGrossTonnageVessel(part.getChoiceGrossTonnageVessel())
                .unitsGrossTonnageVessel(part.getUnitsGrossTonnageVessel())
                .features(new ArrayList<>(part.getFeatures().stream().map(feature -> Feature.builder()
                    .type(feature.getType())
                    .properties(feature.getProperties())
                    .geometry(feature.getGeometry())
                    .build()).collect(toList())))
                .build()).collect(toSet())))
            .attachments(new ArrayList<>(georegulation.getAttachments().stream()
                .map(attachment -> {
                    try {
                        return attachmentService.clone(attachment);
                    } catch (ServiceException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(toList())))
            .build();

        Georegulation savedGeoregulation = repository.save(clonedGeoregulation);

        folderService.addGeoregulationToGeoregulationFolders(id, savedGeoregulation);

        return savedGeoregulation.getId();
    }

    public Set<GeoregulationSelectDto> findAvailableToLinkAsWarningPublicReference(UUID warningId, String username) {
        ServiceUser serviceUser = getServiceUser(username);

        Warning warning = warningRepository.findById(warningId).orElseThrow(NotFoundException::new);

        Serie serie = serieService.findById(warning.getSerie().getId());
        MaritimeService service = serie.getMaritimeService();

        boolean serviceUserHasNoRequiredRoleOnSerieService = checkServiceUserHasNoRequiredRoleOnSerieService(
            serviceUser, service, ServiceRole.SERVICE_OPERATOR, ServiceRole.SERVICE_ADMIN
        );

        if (serviceUserHasNoRequiredRoleOnSerieService) {
            throw new ForbiddenException();
        }

        return warning.getGeoregulations().stream()
            .map(warningGeoregulation -> new GeoregulationSelectDto(warningGeoregulation.getGeoregulation()))
            .collect(toSet());
    }
}
