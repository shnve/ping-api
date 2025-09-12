package fr.shom.ping.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.jpa.impl.JPAQuery;
import fr.shom.ping.exception.BadRequestException;
import fr.shom.ping.exception.ForbiddenException;
import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.*;
import fr.shom.ping.model.dto.AbstractWarningAdminDto;
import fr.shom.ping.model.dto.AttachmentDownloadDto;
import fr.shom.ping.model.dto.FeatureDto;
import fr.shom.ping.model.dto.FolderConcernedDto;
import fr.shom.ping.model.dto.GeneralAreaDto;
import fr.shom.ping.model.dto.InfosourceReferenceDto;
import fr.shom.ping.model.dto.MaritimeEntityConcernedDto;
import fr.shom.ping.model.dto.SerieDto;
import fr.shom.ping.model.dto.WarningAdminDetailDto;
import fr.shom.ping.model.dto.WarningAdminEditDto;
import fr.shom.ping.model.dto.WarningAdminTableDto;
import fr.shom.ping.model.dto.WarningCreateDto;
import fr.shom.ping.model.dto.WarningMaritimeEntityReferenceDto;
import fr.shom.ping.model.dto.WarningReferenceAsPublicReferenceDto;
import fr.shom.ping.model.dto.WarningReferenceDto;
import fr.shom.ping.model.dto.WarningUpdateDto;
import fr.shom.ping.model.dto.WarningUpdatePublishedDto;
import fr.shom.ping.model.enums.DiffusionNavtexEmergencyLevel;
import fr.shom.ping.model.enums.WarningReferenceCategoryEnum;
import fr.shom.ping.model.enums.WarningStatus;
import fr.shom.ping.model.mapper.WarningCreateDtoToWarningMapper;
import fr.shom.ping.model.mapper.WarningToWarningAdminDetailDtoMapper;
import fr.shom.ping.model.predicate.WarningAdminSearchCriteria;
import fr.shom.ping.properties.PingProperties;
import fr.shom.ping.repository.AttachmentRepository;
import fr.shom.ping.repository.GeoregulationRepository;
import fr.shom.ping.repository.MaritimeEntityReferenceRepository;
import fr.shom.ping.repository.MaritimeEntityRepository;
import fr.shom.ping.repository.MaritimeServiceRepository;
import fr.shom.ping.repository.ServiceUserRepository;
import fr.shom.ping.repository.SubAreaRepository;
import fr.shom.ping.repository.WarningLocalityNamesRepository;
import fr.shom.ping.repository.WarningRepository;
import fr.shom.ping.scheduling.SchedulerJobService;
import fr.shom.ping.utils.NavtexUtil;
import fr.shom.ping.utils.TelexUtil;
import fr.shom.ping.utils.search.WarningAdminTableUtil;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableSortedSet.toImmutableSortedSet;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static com.querydsl.core.types.Projections.constructor;
import static fr.shom.ping.exception.ThrowingConsumerWrapper.throwingConsumerWrapper;
import static fr.shom.ping.model.QWarning.warning;
import static fr.shom.ping.model.enums.GlobalWarningType.NOTICE;
import static fr.shom.ping.model.enums.GlobalWarningType.NOTICE_TP;
import static fr.shom.ping.model.enums.SerieType.LOCAL_NAVIGATIONAL_WARNING;
import static fr.shom.ping.model.enums.ServiceRole.SERVICE_ADMIN;
import static fr.shom.ping.model.enums.ServiceRole.SERVICE_OPERATOR;
import static fr.shom.ping.model.enums.UserStatus.ENABLED;
import static fr.shom.ping.model.enums.WarningHazardTypeGeneral.IN_FORCE_BULLETIN;
import static fr.shom.ping.model.enums.WarningReferenceCategoryEnum.CANCELLATION;
import static fr.shom.ping.model.enums.WarningReferenceCategoryEnum.IN_FORCE;
import static fr.shom.ping.model.enums.WarningReferenceCategoryEnum.REPLACEMENT;
import static fr.shom.ping.model.enums.WarningReferenceCategoryEnum.SOURCE_REFERENCE;
import static fr.shom.ping.model.enums.WarningStatus.CANCELLED;
import static fr.shom.ping.model.enums.WarningStatus.DRAFT;
import static fr.shom.ping.model.enums.WarningStatus.MODEL;
import static fr.shom.ping.model.enums.WarningStatus.PUBLISHED;
import static fr.shom.ping.model.mapper.WarningToWarningAdminDetailDtoMapper.getReferencesByCategory;
import static fr.shom.ping.utils.UserUtil.checkServiceUserHasNoRequiredRoleOnSerieService;
import static fr.shom.ping.utils.WarningUtil.checkIfAtLeastInformationOrDetailsIsFilledForEachPart;
import static fr.shom.ping.utils.WarningUtil.checkIfAtLeastOneMandatoryLanguageIsMissing;
import static fr.shom.ping.validation.ValidTimezoneValidator.checkIfUtcPatternIsvalid;
import static java.time.Instant.now;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.IntStream.rangeClosed;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.springframework.util.StringUtils.hasText;


@Slf4j
@RequiredArgsConstructor
@Service
public class WarningAdminService {
    private static final ConstructorExpression<WarningReferenceDto> REFERENCE_PROJECTION = constructor(
        WarningReferenceDto.class,
        warning.id,
        warning.nameOfSeries,
        warning.warningNumber,
        warning.year,
        warning.creationDate,
        warning.publicationTime
    );
    private static final String FEATURE_PART_EXCEPTION = "Information and details are not filled for at least one feature part";
    private static final short END_OF_BROADCAST_DAY_ROUTINE_DEFAULT = 30;
    private static final short END_OF_BROADCAST_DAY_OTHER_DEFAULT = 0;

    private final EntityManager entityManager;
    private final WarningRepository repository;
    private final SerieService serieService;
    private final ProUserService proUserService;
    private final SubAreaRepository subAreaRepository;
    private final WarningLocalityNamesRepository localityNamesRepository;
    private final AttachmentService attachmentService;
    private final ServiceUserRepository serviceUserRepository;
    private final MaritimeServiceRepository maritimeServiceRepository;
    private final WarningToWarningAdminDetailDtoMapper warningToWarningAdminDetailDtoMapper = new WarningToWarningAdminDetailDtoMapper();
    private final WarningCreateDtoToWarningMapper warningCreateDtoToWarningMapper = new WarningCreateDtoToWarningMapper();
    private final SchedulerJobService schedulerJobService;

    private final TelexUtil telexUtil;

    private final NavtexUtil navtexUtil;

    private final InfosourceAdminService infosourceAdminService;

    private final SissyService sissyService;

    private final MaritimeEntityRepository maritimeEntityRepository;

    private final MaritimeEntityReferenceRepository maritimeEntityReferenceRepository;

    private final GeoregulationRepository georegulationRepository;

    private final FolderService folderService;

    private final SseNotificationService sseNotificationService;

    private final SecomService secomService;

    private final AttachmentRepository attachmentRepository;

    private final PingProperties pingProperties;


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

    public PageImpl<WarningAdminTableDto> findAllBySearchCriteria(WarningAdminSearchCriteria searchCriteria, Pageable pageable) {
        return WarningAdminTableUtil.find(entityManager, searchCriteria, pageable);
    }

    @Transactional
    public WarningAdminEditDto create(WarningCreateDto warningCreateDto, String username) {
        final ProUser proUser = proUserService.findByEmail(username);

        // Check user status
        if (proUser.getStatus() != ENABLED) {
            throw new ForbiddenException("Current user is not enabled");
        }

        Serie serie = serieService.findById(warningCreateDto.getSerieId());

        // Check if service can produce warnings
        if (!serie.getMaritimeService().isMaritimeNoticeProducer()) {
            throw new ForbiddenException("Service cannot produce warning");
        }

        // Check if serie belongs to service
        if (!serie.getMaritimeService().getId().equals(warningCreateDto.getServiceId())) {
            throw new ForbiddenException("Serie does not belong to service");
        }

        if (warningCreateDto.isUseAsModel() && !hasText(warningCreateDto.getModelName())) {
            throw new BadRequestException("Model must have a model name");
        }

        // Check general areas against serie's general areas
        Set<GeneralArea> generalAreas = new TreeSet<>(getGeneralAreas(warningCreateDto, serie));

        // References
        Set<WarningReference> warningReferences = null;
        if (!warningCreateDto.isUseAsModel()) {
            warningReferences = getWarningReferences(warningCreateDto, serie);
        }

        // For each parts, check if at least information or typeDetails is filled
        boolean atLeastInformationOrDetailsIsFilledForEachPart = warningCreateDto.getParts().stream()
            .allMatch(part -> !part.getInformation().isEmpty() || !part.getTypeDetails().isEmpty());

        if (!atLeastInformationOrDetailsIsFilledForEachPart) {
            throw new BadRequestException("Information and details are not filled for at least one feature part");
        }

        List<FeaturePart> featureParts = getFeatureParts(warningCreateDto, serie);

        Set<WarningAttachment> attachments = newHashSet();
        updateAttachments(warningCreateDto.getAttachmentIds(), attachments, emptySet());

        Warning warning = this.warningCreateDtoToWarningMapper.apply(warningCreateDto);
        warning.setSerie(serie);
        warning.setNameOfSeries(serie.getName());
        warning.setProductionAgency(serie.getMaritimeService().getProductionAgency());
        warning.setGeneralAreas(new TreeSet<>(generalAreas));

        // if serie maritime service is a s124 data producer, set s124 identifier and ohi producer code
        if (serie.getMaritimeService().isS124DataProducer()) {
            warning.setSerieS124Identifier(serie.getS124Identifier());
            warning.setMaritimeServiceihoProducerCode(serie.getMaritimeService().getIhoProducerCode());
        }

        // Check localities against general areas of serie
        List<WarningLocalityNames> localities = newArrayList(getLocalities(warningCreateDto, generalAreas));
        localities.forEach(locality -> locality.setWarning(warning));
        warning.getLocalities().clear();
        warning.getLocalities().addAll(localities);

        if (!warningCreateDto.isUseAsModel()) {
            warning.setWarningReferences(warningReferences);
        }
        warning.setAttachments(newHashSet(attachments));
        warning.getAttachments().forEach(warningAttachment -> warningAttachment.setWarning(warning));
        warning.setFeatureParts(new TreeSet<>(featureParts));
        warning.setLastUpdate(now());
        warning.setLastUpdateUser(proUser);
        warning.setAddCancellationTimeToTelex(warningCreateDto.isAddCancellationTimeToTelex());

        setEndOfBroadcastDate(warning, warningCreateDto.getNavtexConfig(), false);

        if (warningCreateDto.isUseAsModel()) {
            warning.setStatus(MODEL);
            warning.setModelName(warningCreateDto.getModelName());
        } else {
            warning.setStatus(DRAFT);
        }

        // Check if reserved warning number is available
        Integer warningNumber = warningCreateDto.getWarningNumber();
        Integer year = warningCreateDto.getYear();
        if (warningNumber != null && year != null) {
            if (!isWarningNumberAvailable(serie, warningNumber, year)) {
                throw new BadRequestException("Warning number is reserved for this year");
            }

            warning.setWarningNumber(warningNumber);
            warning.setYear(year);
        }
        repository.save(warning);
        return this.warningToWarningAdminDetailDtoMapper.apply(warning);
    }

    private static void setEndOfBroadcastDate(Warning warning, WarningNavtexConfig navtexConfig, boolean editMode) {
        if (navtexConfig != null) {
            final Short endDay = navtexConfig.getEndDay() != null ? navtexConfig.getEndDay() :
                (DiffusionNavtexEmergencyLevel.ROUTINE.equals(navtexConfig.getEmergencyLevel()) ? END_OF_BROADCAST_DAY_ROUTINE_DEFAULT :
                    END_OF_BROADCAST_DAY_OTHER_DEFAULT);
            final LocalDate startFrom = editMode ? LocalDate.ofInstant(warning.getCreationDate(), UTC) : LocalDate.now();
            warning.setEndOfBroadcastDate(startFrom.plusDays(endDay));
        }
    }

    public boolean isWarningNumberAvailable(Serie serie, Integer warningNumber, Integer year) {
        boolean warningNumberIsAuthorizedInSerieRanges = serie.getRanges()
            .entrySet()
            .stream()
            .anyMatch(range -> warningNumber >= range.getKey() && warningNumber <= range.getValue());

        if (!warningNumberIsAuthorizedInSerieRanges) {
            return false;
        }

        return repository.isWarningNumberAvailable(serie, warningNumber, year);
    }

    public Integer nextAvailableNumber(Serie serie, Integer year) {
        List<Integer> reservedNumbers = repository.findReservedWarningNumbersBySerieAndYear(serie, year);

        return new TreeMap<>(serie.getRanges()).entrySet().stream()
            .flatMap(range -> rangeClosed(range.getKey(), range.getValue()).boxed())
            .filter(availableNumber -> !reservedNumbers.contains(availableNumber))
            .findFirst()
            .orElseThrow(() -> new BadRequestException("No next available number"));
    }

    private List<FeaturePart> getFeatureParts(AbstractWarningAdminDto warningAdminDto, Serie serie) {
        AtomicInteger featurePosition = new AtomicInteger(1);
        List<FeaturePart> featureParts = warningAdminDto.getParts().stream()
            .map(featurePartDto -> {
                AtomicInteger dateRangePosition = new AtomicInteger(1);
                List<FixedDateRange> dateRanges = featurePartDto.getDateRanges();
                dateRanges.forEach(dateRange -> {
                    dateRange.setPosition(dateRangePosition.getAndIncrement());
                    checkIfDateRangeTimeZoneIsValid(dateRange.getTimezone());
                });

                return FeaturePart.builder()
                    .position(featurePosition.getAndIncrement())
                    .geometries(featurePartDto.getGeometries().stream().map(FeatureDto::toFeature).collect(toSet()))
                    .affectedAreas(featurePartDto.getAffectedAreas().stream().map(FeatureDto::toFeature).collect(toSet()))
                    .warningHazardTypeDetails(featurePartDto.getTypeDetails())
                    .information(featurePartDto.getInformation().entrySet()
                        .stream()
                        .map(entry -> LocalizedText.builder().language(entry.getKey()).text(entry.getValue()).build())
                        .collect(toSet()))
                    .fixedDateRanges(new TreeSet<>(dateRanges))
                    .build();
            })
            .collect(toList());

        return featureParts;
    }

    public static void checkIfDateRangeTimeZoneIsValid(String timezone) {
        if (StringUtils.isBlank(timezone)) {
            throw new BadRequestException("validation.warning.period.timezone.not-blank");
        }

        checkIfUtcPatternIsvalid(timezone);
    }

    private Set<WarningReference> getWarningReferences(AbstractWarningAdminDto warningAdminDto, Serie serie) {
        Set<WarningReference> warningReferences = new HashSet<>();

        // Check cancelled warning ids
        List<UUID> cancelledWarningIds = warningAdminDto.getCancelledWarningIds();

        if (isNotEmpty(cancelledWarningIds)) {
            long countCancelledWarningIds = repository.countByIdInAndStatusAndSerie(cancelledWarningIds, PUBLISHED, serie);

            if (countCancelledWarningIds != cancelledWarningIds.size()) {
                throw new BadRequestException("Cancelled warning ids do not match rules");
            }

            List<Warning> cancelledWarnings = repository.findAllById(cancelledWarningIds);
            warningReferences.add(
                WarningReference.builder()
                    .category(CANCELLATION)
                    .warnings(newHashSet(cancelledWarnings))
                    .build()
            );
        }

        // Check replaced warning ids
        List<UUID> replacedWarningIds = warningAdminDto.getReplacedWarningIds();
        if (isNotEmpty(replacedWarningIds)) {
            if (!asList(NOTICE, NOTICE_TP).contains(serie.getType().getGlobalWarningType()) && !replacedWarningIds.isEmpty()) {
                throw new BadRequestException("Replacing warnings is only available for Notice and Notice TP global type");
            }

            long countReplacedWarningIds = repository.countReplacedWarningIds(replacedWarningIds, serie, LOCAL_NAVIGATIONAL_WARNING,
                warningAdminDto.getServiceId(), PUBLISHED);

            if (countReplacedWarningIds != replacedWarningIds.size()) {
                throw new BadRequestException("Replaced warning ids do not match rules");
            }

            List<Warning> replacedWarnings = repository.findAllById(replacedWarningIds);
            warningReferences.add(
                WarningReference.builder()
                    .category(REPLACEMENT)
                    .warnings(newHashSet(replacedWarnings))
                    .build()
            );
        }

        updateParentWarnings(warningAdminDto.getParentWarningIds(), warningReferences);
        return warningReferences;
    }

    private void updateLinkedInfosources(Serie serie, List<UUID> infosourcesIds, Set<Infosource> linkedInfosources) {
        final MaritimeService serieService = serie.getMaritimeService();
        final Set<InfosourceReferenceDto> authorizedInfosources = infosourceAdminService.findByIssuingServiceOrRecipient(serieService);
        final boolean allNewInfosourcesInAuthorizedList = infosourcesIds.stream()
            .allMatch(authorizedInfosources.stream()
                .map(InfosourceReferenceDto::getId)
                .collect(toList())::contains);

        if (!allNewInfosourcesInAuthorizedList) {
            throw new BadRequestException("At least one infosource cannot be linked");
        }

        List<Infosource> infosourcesToLink =
            infosourceAdminService.findAllByIds(infosourcesIds);

        linkedInfosources.removeAll(linkedInfosources.stream()
            .filter(infosource -> !infosourcesToLink.contains(infosource))
            .collect(toSet()));

        linkedInfosources.addAll(newHashSet(infosourcesToLink));
    }

    private void updateLinkedEntities(Serie serie, List<UUID> maritimeEntitiesIds, Set<MaritimeEntity> linkedMaritimeEntities) {
        final MaritimeService serieMaritimeService = serie.getMaritimeService();
        Set<MaritimeEntity> serviceMaritimeEntities = serieMaritimeService.getMaritimeEntities();
        if (isEmpty(serviceMaritimeEntities)) {
            return;
        }

        List<MaritimeEntity> availableMaritimeEntities = maritimeEntityRepository.findAvailableMaritimeEntitiesToLink(serieMaritimeService.getId());

        Set<MaritimeEntity> maritimeEntitiesToLink =
            new HashSet<>(maritimeEntityRepository.findAllById(
                maritimeEntitiesIds.stream()
                    .filter(uuid -> availableMaritimeEntities.stream()
                        .map(MaritimeEntity::getId)
                        .collect(toList())
                        .contains(uuid))
                    .collect(toList())
            ));

        linkedMaritimeEntities.removeAll(
            linkedMaritimeEntities.stream()
                .filter(maritimeEntity ->
                    !maritimeEntitiesToLink.contains(maritimeEntity)
                ).collect(toSet()));
        linkedMaritimeEntities.addAll(maritimeEntitiesToLink);
    }

    private void updateWarningPublicsReferences(Set<WarningReferenceAsPublicReferenceDto> warningReferenceAsPublicReferenceDtos,
                                                Set<WarningPublicReference> warningPublicReferences, Set<WarningReference> warningReferences) {

        Set<WarningPublicReference> availableWarningPublicsReferences = newHashSet();
        warningReferences.forEach(warningReference -> warningReference.getWarnings().forEach(warningReferenceToAdd ->
                availableWarningPublicsReferences.add(
                    WarningPublicReference.builder()
                        .id(UUID.randomUUID())
                        .warningPublicReference(warningReferenceToAdd)
                        .category(warningReference.getCategory())
                        .build())
            )
        );

        Set<WarningPublicReference> warningPublicsReferencesToLink =
            availableWarningPublicsReferences.stream()
                .filter(warningPublicReference -> {
                    WarningReferenceAsPublicReferenceDto warningReferenceAsPublicReferenceDto =
                        WarningReferenceAsPublicReferenceDto.builder()
                            .warningId(warningPublicReference.getWarningPublicReference().getId())
                            .category(warningPublicReference.getCategory()).build();


                    return warningReferenceAsPublicReferenceDtos.contains(warningReferenceAsPublicReferenceDto);
                })
                .collect(toSet());

        warningPublicReferences.removeIf(warningPublicReferenceToRemove ->
            warningPublicReferences.stream().noneMatch(warningPublicReference ->
                warningPublicReference.getWarningPublicReference().getId() == warningPublicReferenceToRemove.getWarningPublicReference().getId()
                    && warningReferenceAsPublicReferenceDtos.stream()
                    .map(WarningReferenceAsPublicReferenceDto::getWarningId)
                    .collect(toSet())
                    .contains(warningPublicReference.getWarningPublicReference().getId())
                    && warningPublicReference.getCategory() == warningPublicReferenceToRemove.getCategory())
                || warningReferenceAsPublicReferenceDtos.stream().noneMatch(warningReferenceAsPublicReferenceDto ->
                warningReferenceAsPublicReferenceDto.getWarningId().equals(warningPublicReferenceToRemove.getWarningPublicReference().getId())
                    && warningReferenceAsPublicReferenceDto.getCategory() == warningPublicReferenceToRemove.getCategory())
        );

        warningPublicReferences.removeIf(warningPublicReferenceToRemove ->
            availableWarningPublicsReferences.stream().noneMatch(availableWarningPublicReference ->
                availableWarningPublicReference.getWarningPublicReference().getId() == warningPublicReferenceToRemove.getWarningPublicReference().getId()
                    && availableWarningPublicReference.getCategory() == warningPublicReferenceToRemove.getCategory())
        );

        warningPublicReferences.forEach(warningPublicReference ->
            warningPublicsReferencesToLink.removeIf(warningPublicReferenceToLink ->
                warningPublicReference.getWarningPublicReference().getId() == warningPublicReferenceToLink.getWarningPublicReference().getId()
                    && warningPublicReference.getCategory() == warningPublicReferenceToLink.getCategory())

        );

        warningPublicReferences.addAll(warningPublicsReferencesToLink);
    }

    private Set<MaritimeEntityReference> getEntitiesReference(Serie serie, Set<MaritimeEntity> linkedMaritimeEntities, Set<UUID> entityReferenceIds) {
        final MaritimeService serieMaritimeService = serie.getMaritimeService();
        Set<MaritimeEntity> serviceMaritimeEntities = serieMaritimeService.getMaritimeEntities();
        if (isEmpty(serviceMaritimeEntities)) {
            return emptySet();
        }


        Set<MaritimeEntityReference> availableMaritimeEntitiesReferences = newHashSet();
        linkedMaritimeEntities.forEach(maritimeEntity -> availableMaritimeEntitiesReferences.addAll(maritimeEntity.getReferences().stream()
            .filter(MaritimeEntityReference::isPublic)
            .collect(toSet())));

        return availableMaritimeEntitiesReferences.stream()
            .filter(maritimeEntityReference -> entityReferenceIds.contains(maritimeEntityReference.getId()))
            .collect(toSet());
    }

    private void updateLinkedGeoregulations(Serie serie, List<UUID> georegulationIds, Set<WarningGeoregulation> linkedGeoregulations,
                                            Set<UUID> georegulationReferenceIds) {
        final MaritimeService service = serie.getMaritimeService();
        List<Georegulation> availableGeoregulations = georegulationRepository.findByIssuingServiceOrRecipient(service);

        Set<Georegulation> georegulationsToLink =
            newHashSet(georegulationRepository.findAllById(
                georegulationIds.stream()
                    .filter(uuid -> availableGeoregulations.stream()
                        .map(Georegulation::getId)
                        .collect(toList())
                        .contains(uuid))
                    .collect(toList())
            ));

        Set<WarningGeoregulation> warningGeoregulationsToLink = newHashSet();
        georegulationsToLink.forEach(georegulation -> warningGeoregulationsToLink.add(
            WarningGeoregulation.builder()
                .georegulation(georegulation)
                .build()
        ));

        linkedGeoregulations.removeAll(
            linkedGeoregulations.stream()
                .filter(warningGeoregulation -> !georegulationsToLink.contains(warningGeoregulation.getGeoregulation()))
                .collect(toSet()));

        warningGeoregulationsToLink.removeAll(
            warningGeoregulationsToLink.stream()
                .filter(warningGeoregulation ->
                    linkedGeoregulations.stream()
                        .map(linkedWarningGeoregulation -> linkedWarningGeoregulation.getGeoregulation().getId())
                        .collect(toList())
                        .contains(warningGeoregulation.getGeoregulation().getId()))
                .collect(toSet()));

        linkedGeoregulations.addAll(warningGeoregulationsToLink);
        linkedGeoregulations.forEach(warningGeoregulation ->
            warningGeoregulation.setIsPublicReference(georegulationReferenceIds.contains(warningGeoregulation.getGeoregulation().getId()))
        );
    }

    private void updateParentWarnings(List<UUID> parentWarningIds, Set<WarningReference> warningReferences) {
        if (isNotEmpty(parentWarningIds)) {
            long countParentWarningIds = repository.countByIdInAndStatusIn(parentWarningIds, asList(WarningStatus.values()));

            if (countParentWarningIds != parentWarningIds.size()) {
                throw new BadRequestException("Parent warning ids do not match rules");
            }

            List<Warning> parentWarnings = repository.findAllById(parentWarningIds);
            warningReferences.add(
                WarningReference.builder()
                    .category(SOURCE_REFERENCE)
                    .warnings(newHashSet(parentWarnings))
                    .build()
            );
        }
    }

    private void updateAttachments(List<UUID> attachmentIds, Set<WarningAttachment> warningAttachments, Set<UUID> attachmentReferenceIds) {
        Set<Attachment> attachmentsToLink = new HashSet<>();
        if (isNotEmpty(attachmentIds)) {
            attachmentsToLink = new HashSet<>(this.attachmentService.findAllByIdIn(attachmentIds));
        }

        Set<WarningAttachment> warningAttachmentsToLink = newHashSet();
        attachmentsToLink.forEach(attachment -> warningAttachmentsToLink.add(
            WarningAttachment.builder()
                .attachment(attachment)
                .build())
        );

        Set<Attachment> finalAttachmentsToLink = attachmentsToLink;
        warningAttachments.removeAll(
            warningAttachments.stream()
                .filter(warningAttachment -> !finalAttachmentsToLink.contains(warningAttachment.getAttachment()))
                .collect(toSet()));

        warningAttachmentsToLink.removeAll(
            warningAttachmentsToLink.stream()
                .filter(warningAttachment ->
                    warningAttachments.stream()
                        .map(linkedWarningAttachment -> linkedWarningAttachment.getAttachment().getId())
                        .collect(toList())
                        .contains(warningAttachment.getAttachment().getId()))
                .collect(toSet()));

        warningAttachments.addAll(warningAttachmentsToLink);
        warningAttachments.forEach(
            warningAttachment -> warningAttachment.setIsPublicReference(attachmentReferenceIds.contains(warningAttachment.getAttachment().getId()))
        );
    }

    private List<GeneralArea> getGeneralAreas(AbstractWarningAdminDto warningAdminDto, Serie serie) {
        List<SubArea> foundGeneralAreas = subAreaRepository.findAllByIdIn(warningAdminDto.getGeneralAreas().stream().map(GeneralAreaDto::getId).toList());
        List<UUID> generalAreaIdsFromAllGeneralAreas = serie.getSubAreas().stream()
            .map(SubArea::getId)
            .toList();
        boolean atLeastOneGeneralAreaDoesNotBelongToSerie = !foundGeneralAreas.stream()
            .map(SubArea::getId)
            .allMatch(generalAreaIdsFromAllGeneralAreas::contains);

        if (warningAdminDto.getGeneralAreas().size() != foundGeneralAreas.size() || atLeastOneGeneralAreaDoesNotBelongToSerie) {
            throw new BadRequestException("At least one general area does not belong to serie");
        }

        return foundGeneralAreas.stream()
            .map(subArea -> GeneralArea.builder()
                .id(subArea.getId())
                .name(subArea.getName())
                .internationalName(subArea.getInternationalName())
                .subArea(subArea)
                .position(warningAdminDto.getGeneralAreas().stream().filter(generalAreaDto -> generalAreaDto.getId().equals(subArea.getId())).findFirst().get().getPosition())
                .build())
            .collect(toList());
    }

    private List<WarningLocalityNames> getLocalities(AbstractWarningAdminDto warningAdminDto, Set<GeneralArea> subAreaParents) {
        boolean atLeastOneLocalityDoesNotBelongToAtLeastOneParent = warningAdminDto.getLocalityNames().stream()
            .anyMatch(locality -> !subAreaParents.stream()
                .map(generalArea -> generalArea.getSubArea().getId())
                .collect(toSet())
                .contains(locality.getParentId()));

        if (atLeastOneLocalityDoesNotBelongToAtLeastOneParent) {
            throw new BadRequestException("At least one locality does not belong to subarea of serie");
        }

        final List<WarningLocalityNames> localityNames = new ArrayList<>();
        subAreaParents.stream().sorted(comparing(GeneralArea::getPosition)).forEach(parent -> {
            final List<WarningLocalityNames> localities = warningAdminDto.getLocalityNames().stream()
                .filter(warningLocalityName -> warningLocalityName.getParentId().equals(parent.getId()))
                .map(locality -> WarningLocalityNames.builder()
                    .id(locality.getId())
                    .position(
                        Optional.ofNullable(
                            warningAdminDto.getLocalityNames().stream()
                                .filter(warningLocalityName -> warningLocalityName.equals(locality))
                                .findFirst().get().getPosition()
                        )
                        .orElseThrow(() -> new BadRequestException("locality " + locality.getName() + "with id " + locality.getId() + " has no position"))
                    )
                    .name(locality.getName())
                    .internationalName(ofNullable(locality.getInternationalName()).orElse(locality.getName()))
                    .parent(parent.getSubArea())
                    .build()
                )
                .toList();

            localityNames.addAll(localities);
        });

        return localityNames;
    }

    @Transactional
    public WarningAdminEditDto update(UUID id, WarningUpdateDto warningUpdateDto, String username) {
        final ProUser proUser = proUserService.findByEmail(username);

        // Check user status
        if (proUser.getStatus() != ENABLED) {
            throw new ForbiddenException("Current user is not enabled");
        }

        final Warning warning = this.repository.findById(id).orElseThrow(NotFoundException::new);

        if (warning.getStatus() != DRAFT && warning.getStatus() != MODEL) {
            throw new BadRequestException("Warning must be in DRAFT or MODEL status");
        }

        if (warning.getStatus() == MODEL && !hasText(warningUpdateDto.getModelName())) {
            throw new BadRequestException("Warning must have a model name");
        }

        final Serie serie = warning.getSerie();

        boolean warningNumberHasChanged = false;
        Integer newWarningNumber = null;
        Integer newYear = null;
        if (warning.getStatus() == DRAFT) {
            final Integer currentWarningNumber = warning.getWarningNumber();
            final Integer currentYear = warning.getYear();
            newWarningNumber = warningUpdateDto.getWarningNumber();
            newYear = warningUpdateDto.getYear();
            warningNumberHasChanged = !ObjectUtils.nullSafeEquals(currentWarningNumber, newWarningNumber)
                || !ObjectUtils.nullSafeEquals(currentYear, newYear);
        }

        // copy properties
        warning.setWarningHazardTypeGeneral(warningUpdateDto.getTypeGeneral());
        warning.setEstimatedPublicationDate(warningUpdateDto.getEstimatedPublicationDate());
        warning.setReviewDate(warningUpdateDto.getReviewDate());
        warning.setCancellationDate(ofNullable(warningUpdateDto.getCancellationTime()).map(cancellationTime -> cancellationTime.truncatedTo(MINUTES)).orElse(null));
        warning.setInternalComment(warningUpdateDto.getInternalComment());
        warning.setLastUpdate(now());
        warning.setLastUpdateUser(proUser);

        if (warning.getStatus() == DRAFT) {
            // check chosen warning number is available
            // if warning number / year has changed
            if (warningNumberHasChanged && newWarningNumber != null && newYear != null) {
                if (!isWarningNumberAvailable(serie, newWarningNumber, newYear)) {
                    throw new BadRequestException("Warning number is reserved for this year");
                }
            }
        }

        if (warning.getStatus() == MODEL) {
            warning.setModelName(warningUpdateDto.getModelName());
        }

        warning.setNavtexConfig(warningUpdateDto.getNavtexConfig());
        if (warning.getNavtexConfig() != null) {
            warning.getNavtexConfig().setWarning(warning);
        }
        if (warning.getStatus() == DRAFT) {
            warning.setWarningNumber(newWarningNumber);
            warning.setYear(newYear);
        }

        // Check general areas against serie's general areas
        Set<GeneralArea> generalAreas = new HashSet<>(getGeneralAreas(warningUpdateDto, serie));
        generalAreas.forEach(generalArea -> generalArea.setWarning(warning));
        warning.getGeneralAreas().clear();
        warning.getGeneralAreas().addAll(generalAreas);

        // Check localities against general areas of serie
        List<WarningLocalityNames> localities = newArrayList(getLocalities(warningUpdateDto, generalAreas));
        localities.forEach(locality -> locality.setWarning(warning));
        warning.getLocalities().clear();
        warning.getLocalities().addAll(localities);

        if (warning.getStatus() == DRAFT) {
            // References
            Set<WarningReference> warningReferences = getWarningReferences(warningUpdateDto, serie);
            warningReferences.forEach(warningReference -> warningReference.setWarning(warning));
            warning.getWarningReferences().clear();
            warning.getWarningReferences().addAll(warningReferences);

            Set<Infosource> linkedInfosources = newHashSet(warning.getInfosources());
            updateLinkedInfosources(serie, warningUpdateDto.getInfosourcesIds(), linkedInfosources);
            warning.getInfosources().clear();
            warning.getInfosources().addAll(linkedInfosources);

            Set<MaritimeEntity> linkedMaritimeEntities = newHashSet(warning.getMaritimeEntities());
            updateLinkedEntities(serie, warningUpdateDto.getMaritimeEntitiesIds(), linkedMaritimeEntities);
            warning.getMaritimeEntities().clear();
            warning.getMaritimeEntities().addAll(linkedMaritimeEntities);

            Set<MaritimeEntityReference> maritimeEntityReferences = getEntitiesReference(serie, linkedMaritimeEntities,
                warningUpdateDto.getEntityReferenceIds());
            warning.getEntityReferences().clear();
            warning.getEntityReferences().addAll(maritimeEntityReferences);

            Set<WarningGeoregulation> linkedGeoregulations = newHashSet(warning.getGeoregulations());
            updateLinkedGeoregulations(serie, warningUpdateDto.getGeoregulationIds(), linkedGeoregulations, warningUpdateDto.getGeoregulationReferenceIds());
            linkedGeoregulations.forEach(warningGeoregulation -> warningGeoregulation.setWarning(warning));
            warning.getGeoregulations().clear();
            warning.getGeoregulations().addAll(linkedGeoregulations);

            Set<WarningPublicReference> warningPublicReferences = newHashSet(warning.getWarningsPublicsReferences());
            updateWarningPublicsReferences(warningUpdateDto.getWarningPublicsReferences(), warningPublicReferences, warning.getWarningReferences());
            warningPublicReferences.forEach(warningPublicReference -> warningPublicReference.setWarning(warning));
            warning.getWarningsPublicsReferences().clear();
            warning.getWarningsPublicsReferences().addAll(warningPublicReferences);
        }

        warning.setAddCancellationTimeToTelex(warningUpdateDto.isAddCancellationTimeToTelex());

        SortedSet<FeaturePart> featureParts = new TreeSet<>(getFeatureParts(warningUpdateDto, serie));
        featureParts.forEach(featurePart -> featurePart.setWarning(warning));
        warning.getFeatureParts().clear();
        warning.getFeatureParts().addAll(featureParts);

        // delete unused attachments on disk
        this.deleteUnusedAttachments(warning, warningUpdateDto.getAttachmentIds());

        Set<WarningAttachment> attachments = newHashSet(warning.getAttachments());
        updateAttachments(warningUpdateDto.getAttachmentIds(), attachments, warningUpdateDto.getAttachmentReferenceIds());
        attachments.forEach(warningAttachment -> warningAttachment.setWarning(warning));
        warning.getAttachments().clear();
        warning.getAttachments().addAll(attachments);

        setEndOfBroadcastDate(warning, warningUpdateDto.getNavtexConfig(), true);

        // persist changes
        Warning savedWarning = entityManager.merge(warning);

        if (savedWarning.getStatus() == DRAFT) {
            folderService.addWarningLinkedNotionsToWarningFolders(savedWarning);
        }

        return this.warningToWarningAdminDetailDtoMapper.apply(savedWarning);
    }

    @Transactional
    public WarningAdminEditDto updatePublished(UUID id, WarningUpdatePublishedDto dto, String username) {
        final ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);

        // Check user status
        if (serviceUser.getStatus() != ENABLED) {
            throw new ForbiddenException("Current user is not enabled");
        }

        Warning warning = repository.findById(id).orElseThrow(NotFoundException::new);
        if (warning.getStatus() != PUBLISHED) {
            throw new BadRequestException("Warning must be in PUBLISHED status");
        }

        // update references
        Set<WarningReference> warningReferences = warning.getWarningReferences().stream()
            .filter(warningReference -> warningReference.getCategory() != SOURCE_REFERENCE)
            .collect(toSet());
        updateParentWarnings(dto.getParentWarningIds(), warningReferences);
        warningReferences.forEach(warningReference -> warningReference.setWarning(warning));
        warning.getWarningReferences().clear();
        warning.getWarningReferences().addAll(warningReferences);

        Set<Infosource> linkedInfosources = newHashSet(warning.getInfosources());
        updateLinkedInfosources(warning.getSerie(), dto.getInfosourcesIds(), linkedInfosources);
        warning.getInfosources().clear();
        warning.getInfosources().addAll(linkedInfosources);

        Set<MaritimeEntity> linkedMaritimeEntities = newHashSet(warning.getMaritimeEntities());
        updateLinkedEntities(warning.getSerie(), dto.getMaritimeEntitiesIds(), linkedMaritimeEntities);
        warning.getMaritimeEntities().clear();
        warning.getMaritimeEntities().addAll(linkedMaritimeEntities);

        Set<MaritimeEntityReference> maritimeEntityReferences = getEntitiesReference(warning.getSerie(), linkedMaritimeEntities,
            dto.getEntityReferenceIds());
        warning.getEntityReferences().clear();
        warning.getEntityReferences().addAll(maritimeEntityReferences);

        Set<WarningGeoregulation> linkedGeoregulations = newHashSet(warning.getGeoregulations());
        updateLinkedGeoregulations(warning.getSerie(), dto.getGeoregulationIds(), linkedGeoregulations, dto.getGeoregulationReferenceIds());
        linkedGeoregulations.forEach(warningGeoregulation -> warningGeoregulation.setWarning(warning));
        warning.getGeoregulations().clear();
        warning.getGeoregulations().addAll(linkedGeoregulations);

        Set<WarningPublicReference> warningPublicReferences = newHashSet(warning.getWarningsPublicsReferences());
        updateWarningPublicsReferences(dto.getWarningPublicsReferences(), warningPublicReferences, warning.getWarningReferences());
        warningPublicReferences.forEach(warningPublicReference -> warningPublicReference.setWarning(warning));
        warning.getWarningsPublicsReferences().clear();
        warning.getWarningsPublicsReferences().addAll(warningPublicReferences);

        deleteUnusedAttachments(warning, dto.getAttachmentIds());
        Set<WarningAttachment> attachments = newHashSet(warning.getAttachments());
        updateAttachments(dto.getAttachmentIds(), attachments, dto.getAttachmentReferenceIds());
        attachments.forEach(warningAttachment -> warningAttachment.setWarning(warning));
        warning.getAttachments().clear();
        warning.getAttachments().addAll(attachments);

        // update other simple attributes
        warning.setReviewDate(dto.getReviewDate());
        warning.setInternalComment(dto.getInternalComment());
        warning.setLastUpdate(now());
        warning.setLastUpdateUser(serviceUser);

        Warning savedWarning = entityManager.merge(warning);

        folderService.addWarningLinkedNotionsToWarningFolders(savedWarning);

        return this.warningToWarningAdminDetailDtoMapper.apply(savedWarning);
    }

    /**
     * delete files from disk using AttachmentService.delete()
     *
     * @param warning               the warning from db
     * @param updatedAttachmentList attachment list to update
     */
    public void deleteUnusedAttachments(Warning warning, List<UUID> updatedAttachmentList) {
        final List<UUID> formerAttachmentList = warning.getAttachments().stream()
            .map(warningAttachment -> warningAttachment.getAttachment().getId())
            .collect(toList());
        final List<UUID> deletedAttachments = findDifferences(formerAttachmentList, updatedAttachmentList);

        // delete files from disk using AttachmentService.delete()
        deletedAttachments.forEach(throwingConsumerWrapper(this.attachmentService::deleteFromDisk));
    }

    public List<WarningReferenceDto> findCancellable(UUID serieId) {
        JPAQuery<WarningReferenceDto> query = new JPAQuery<>(entityManager)
            .select(REFERENCE_PROJECTION)
            .from(warning)
            .where(warning.status.eq(PUBLISHED))
            .where(warning.serie.id.eq(serieId));

        return query.fetch();
    }

    public List<WarningReferenceDto> findReplaceable(UUID serieId, String username) {
        Optional<ServiceUser> serviceUser = serviceUserRepository.findByEmail(username);
        if (serviceUser.isEmpty()) {
            throw new ForbiddenException();
        }

        JPAQuery<WarningReferenceDto> query = new JPAQuery<>(entityManager)
            .select(REFERENCE_PROJECTION)
            .from(warning)
            .where(warning.status.eq(PUBLISHED))
            .where(warning.serie.id.eq(serieId)
                .or(warning.serie.type.eq(LOCAL_NAVIGATIONAL_WARNING)
                    .and(warning.serie.maritimeService.eq(serviceUser.get().getService()))));

        return query.fetch();
    }

    public List<WarningReferenceDto> findReferenceable() {
        JPAQuery<WarningReferenceDto> query = new JPAQuery<>(entityManager)
            .select(REFERENCE_PROJECTION)
            .from(warning)
            .where(warning.status.ne(MODEL));

        return query.fetch();
    }

    public WarningAdminEditDto findOne(UUID id) {
        Warning warning = repository.findById(id).orElseThrow(NotFoundException::new);
        return warningToWarningAdminDetailDtoMapper.apply(warning);
    }

    public WarningAdminDetailDto findByIdAndMapForDetailsView(UUID id, String username) {
        final ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);
        final Warning warning = repository.findById(id).orElseThrow(NotFoundException::new);
        final boolean canBeAddedToFolder = warning.getStatus() != DRAFT && maritimeServiceRepository.hasAnyFolderManagerByIds(
            serviceUser.getRoles().stream()
                .filter(serviceUserRole -> asList(SERVICE_OPERATOR, SERVICE_ADMIN).contains(serviceUserRole.getServiceRole()))
                .map(ServiceUserRole::getServiceId)
                .collect(toList())
        );
        return new WarningAdminDetailDto().fromWarning(warning, canBeAddedToFolder);
    }

    @Transactional
    public void publish(UUID id, String username) throws ServiceException {
        Warning warning = repository.findById(id).orElseThrow(NotFoundException::new);
        Serie serie = warning.getSerie();
        ProUser proUser = checkAndGetProUser(username, serie);

        if (warning.getStatus() != DRAFT) {
            throw new BadRequestException("Current warning status is invalid");
        }

        if (isEmpty(warning.getFeatureParts())) {
            throw new BadRequestException("Warning should have at least one feature part");
        }

        boolean atLeastInformationOrDetailsIsFilledForEachPart = checkIfAtLeastInformationOrDetailsIsFilledForEachPart(warning);

        if (!atLeastInformationOrDetailsIsFilledForEachPart) {
            throw new BadRequestException(FEATURE_PART_EXCEPTION);
        }

        if (isNotEmpty(warning.getFeatureParts())) {
            boolean atLeastOneMandatoryLanguageIsMissing = checkIfAtLeastOneMandatoryLanguageIsMissing(warning);

            if (atLeastOneMandatoryLanguageIsMissing) {
                throw new BadRequestException("At least one mandatory language is missing");
            }
        }

        if (isEmpty(warning.getGeneralAreas())) {
            throw new BadRequestException("Warning should have at least one general area");
        }

        if (!NavtexUtil.isConfigValid(warning.getNavtexConfig(), serie.getNavtexSource())) {
            throw new BadRequestException("Warning should have valid NAVTEX configuration");
        }

        // set "IN_FORCE" warning reference if warning hazard type general is IN_FORCE_BULLETIN
        if (IN_FORCE_BULLETIN.equals(warning.getWarningHazardTypeGeneral())) {
            List<Warning> inForceWarnings = repository.findAllByStatusAndSerie(PUBLISHED, warning.getSerie());
            warning.getWarningReferences().add(
                WarningReference.builder()
                    .category(IN_FORCE)
                    .warnings(newHashSet(inForceWarnings))
                    .warning(warning)
                    .build()
            );
        }

        Instant now = now();
        warning.setLastUpdate(now);
        warning.setPublicationTime(now);
        warning.setLastUpdateUser(proUser);
        warning.setStatus(PUBLISHED);

        Integer warningNumber = warning.getWarningNumber();
        if (warningNumber == null) {
            int year = Year.now(UTC).getValue();
            warning.setWarningNumber(nextAvailableNumber(serie, year));
            warning.setYear(year);
        }

        // generate TELEX and update warning
        warning.setTelex(telexUtil.generateTelex(warning));

        // if required, generate NAVTEX and update warning
        if (serie.getNavtexSource() != null) {
            warning.setNavtex(navtexUtil.generateNavtex(warning));
            try {
                navtexUtil.generatePublishedNavtex(warning, pingProperties.getGeneratedNavtexFolder());
            } catch (IOException e) {
                throw new ServiceException("Failed to generate NAVTEX publish file", e);
            }
        }

        repository.save(warning);

        // if warning issuing maritime service is s124 data producer, generate Exchange Set
        generateExchangeSet(warning);

        // notify SISSY
        sissyService.sendWarningPublishedOrCancelled(warning);

        if (warning.getCancellationDate() != null) {
            schedulerJobService.scheduleAutoCancelWarningJob(warning);
        }

        cancelAutomaticallyWarningsInvalidatedByPublishedWarning(warning);

        sseNotificationService.sendWarningNotification(warning);
    }

    @Async
    public void generateExchangeSet(Warning warning) {
        if (!checkWarningS124Capability(warning)) {
            return;
        }
        try {
            secomService.generatePublishedExchangeSetFile(pingProperties.getGeneratedS124Folder(), warning);
        } catch (IOException | ServiceException e) {
            throw new BadRequestException("Failed to generate Exchange Set: " + e.getMessage());
        }
    }

    private static boolean checkWarningS124Capability(Warning warning) {
        if (!warning.getSerie().getMaritimeService().isS124DataProducer() || StringUtils.isBlank(warning.getSerieS124Identifier())) {
            return false;
        }

        if (StringUtils.isBlank(warning.getMaritimeServiceihoProducerCode())) {
            throw new ForbiddenException("Service cannot produce S124 data because no IHO producer code defined");
        }

        return true;
    }

    private void cancelAutomaticallyWarningsInvalidatedByPublishedWarning(Warning warning) {
        List<UUID> warningIdsToCancel = warning.getWarningReferences().stream()
            .filter(warningReference -> warningReference.getCategory() == CANCELLATION || warningReference.getCategory() == REPLACEMENT)
            .map(WarningReference::getWarnings)
            .flatMap(Collection::stream)
            .map(Warning::getId)
            .collect(toList());

        if (isNotEmpty(warningIdsToCancel)) {
            repository.autoCancel(warningIdsToCancel, now().atZone(UTC).toInstant());

            // notify SISSY
            final List<Warning> cancelledWarnings = repository.findAllById(warningIdsToCancel);
            cancelledWarnings.forEach(cancelledWarning -> {
                sissyService.sendWarningPublishedOrCancelled(cancelledWarning);

                // generate NAVTEX cancellation file
                try {
                    generateNavtexCancellationFile(cancelledWarning);
                } catch (IOException e) {
                    log.error("Failed to generate NAVTEX cancellation file", e);
                }
            });
        }
    }

    @Transactional
    public void delete(UUID id, String username) {
        Warning warning = repository.findById(id).orElseThrow(NotFoundException::new);
        checkAndGetProUser(username, warning.getSerie());

        if (warning.getStatus() != DRAFT && warning.getStatus() != MODEL) {
            throw new BadRequestException("Current warning status is invalid");
        }

        repository.delete(warning);
    }

    private ProUser checkAndGetProUser(String username, Serie serie) {
        ProUser proUser = proUserService.findByEmail(username);

        if (proUser == null) {
            throw new ForbiddenException();
        }

        if (proUser instanceof ServiceUser serviceUser) {
            UUID serviceId = serie.getMaritimeService().getId();
            if (serviceUser.getRoles().stream()
                .noneMatch(role -> role.getServiceId().equals(serviceId) &&
                    asList(SERVICE_ADMIN, SERVICE_OPERATOR).contains(role.getServiceRole()))) {
                throw new ForbiddenException("User has no mandatory role for serviceId " + serviceId);
            }
        }
        return proUser;
    }

    @Transactional
    public UUID cancel(UUID id, String username) throws ServiceException {
        final Warning warning = repository.findById(id).orElseThrow(NotFoundException::new);
        final Serie serie = warning.getSerie();
        ProUser proUser = checkAndGetProUser(username, serie);

        if (warning.getStatus() != PUBLISHED) {
            throw new BadRequestException("Current warning status is invalid");
        }

        final Instant now = now();
        warning.setLastUpdate(now);
        warning.setCancellationDate(now);
        warning.setLastUpdateUser(proUser);
        warning.setStatus(CANCELLED);

        final Warning savedWarning = repository.save(warning);

        // notify SISSY
        sissyService.sendWarningPublishedOrCancelled(savedWarning);

        // generate NAVTEX cancellation file
        try {
            generateNavtexCancellationFile(savedWarning);
        } catch (IOException e) {
            throw new ServiceException("Failed to generate NAVTEX cancellation file", e);
        }

        return savedWarning.getId();
    }

    @Transactional
    public void autoCancel(UUID id) throws ServiceException {
        // cancel warning
        repository.autoCancel(singletonList(id), now().atZone(UTC).toInstant());

        // notify SISSY
        final Warning warning = repository.findById(id).orElseThrow(NotFoundException::new);
        sissyService.sendWarningPublishedOrCancelled(warning);

        try {
            generateNavtexCancellationFile(warning);
        } catch (IOException e) {
            throw new ServiceException("Failed to generate NAVTEX cancellation file", e);
        }
    }

    public void generateNavtexCancellationFile(Warning warning) throws IOException {
        final Serie serie = warning.getSerie();
        if (serie.getNavtexSource() == null) {
            return;
        }
        warning.setNavtex(navtexUtil.generateCancellationNavtex(warning, pingProperties.getGeneratedNavtexFolder()));
        repository.save(warning);
    }

    @Transactional
    public UUID clone(UUID id, UUID serieId, WarningReferenceCategoryEnum warningReferenceCategoryEnum, WarningStatus warningStatus, String username) {
        final Warning warning = repository.findById(id).orElseThrow(NotFoundException::new);
        ProUser proUser = proUserService.findByEmail(username);

        List<SerieDto> availableSeries = serieService.findByUserWithAdminOrOperatorRole(username);

        if (availableSeries.stream()
            .map(SerieDto::getId)
            .noneMatch(serieDtoId -> serieId.equals(serieDtoId))) {
            throw new ForbiddenException();
        }

        if (warningStatus != null && warningStatus != DRAFT && warningStatus != MODEL) {
            throw new BadRequestException("Warning status must be DRAFT or MODEL.");
        }

        Serie newSerie = serieService.findById(serieId);

        Warning clonedWarning = new Warning();
        clonedWarning.setSerie(newSerie);
        clonedWarning.setCountry(warning.getCountry());
        clonedWarning.setNameOfSeries(newSerie.getName());
        clonedWarning.setProductionAgency(newSerie.getMaritimeService().getProductionAgency());
        clonedWarning.setGeneralAreas(cloneGeneralAreas(warning, newSerie));
        clonedWarning.setLocalities(cloneLocalities(warning, clonedWarning));
        clonedWarning.setWarningHazardTypeGeneral(warning.getWarningHazardTypeGeneral());
        clonedWarning.setFeatureParts(cloneFeatureParts(warning, newSerie));
        clonedWarning.setInternalComment(warning.getInternalComment());

        // if serie maritime service is a s124 data producer, set s124 identifier and ohi producer code
        if (newSerie.getMaritimeService().isS124DataProducer()) {
            clonedWarning.setSerieS124Identifier(newSerie.getS124Identifier());
            clonedWarning.setMaritimeServiceihoProducerCode(newSerie.getMaritimeService().getIhoProducerCode());
        }

        if (warningStatus != null) {
            clonedWarning.setStatus(warningStatus);
        } else {
            clonedWarning.setStatus(DRAFT);
        }

        Instant now = now();
        clonedWarning.setCreationDate(now);
        clonedWarning.setLastUpdate(now);
        clonedWarning.setLastUpdateUser(proUser);

        if (warning.getStatus() != MODEL && (warningStatus == DRAFT || warningStatus == null)) {
            if (warningReferenceCategoryEnum != null) {
                clonedWarning.getWarningReferences().add(WarningReference.builder()
                    .category(warningReferenceCategoryEnum)
                    .noMessageOnHand(false)
                    .warnings(new HashSet<>(singletonList(warning)))
                    .build());
            }

            clonedWarning.getWarningReferences().add(WarningReference.builder()
                .category(SOURCE_REFERENCE)
                .noMessageOnHand(false)
                .warnings(new HashSet<>(singletonList(warning)))
                .build());
        }

        Warning savedWarning = repository.save(clonedWarning);

        if (warning.getStatus() != MODEL && (warningStatus == DRAFT || warningStatus == null)) {
            folderService.addWarningToWarningFolders(id, savedWarning);
        }

        return savedWarning.getId();
    }

    private ImmutableSortedSet<FeaturePart> cloneFeatureParts(Warning warning, Serie newSerie) {
        List<String> supportedLanguages = Stream.of(newSerie.getMandatoryLanguages(), newSerie.getOptionalLanguages())
            .flatMap(Collection::stream)
            .collect(toList());
        return warning.getFeatureParts().stream()
            .map(featurePart -> {
                FeaturePart clonedFeaturePart = new FeaturePart();
                clonedFeaturePart.setPosition(featurePart.getPosition());
                clonedFeaturePart.setGeometries(new HashSet<>(featurePart.getGeometries()));
                clonedFeaturePart.setAffectedAreas(new HashSet<>(featurePart.getAffectedAreas()));
                clonedFeaturePart.setFixedDateRanges(new TreeSet<>(featurePart.getFixedDateRanges()));
                Set<LocalizedText> filteredInformationByNewSerieSupportedLanguages = featurePart.getInformation().stream()
                    .filter(information -> supportedLanguages.contains(information.getLanguage()))
                    .collect(toSet());
                clonedFeaturePart.setInformation(new HashSet<>(filteredInformationByNewSerieSupportedLanguages));
                clonedFeaturePart.setWarningHazardTypeDetails(new HashSet<>(featurePart.getWarningHazardTypeDetails()));
                return clonedFeaturePart;
            })
            .collect(toImmutableSortedSet(comparing(FeaturePart::getPosition)));
    }

    private ImmutableList<WarningLocalityNames> cloneLocalities(Warning warning, Warning clonedWarning) {
        return warning.getLocalities().stream()
            .map(locality -> WarningLocalityNames.builder()
                .position(locality.getPosition())
                .name(locality.getName())
                .internationalName(ofNullable(locality.getInternationalName()).orElse(locality.getName()))
                .parent(locality.getParent())
                .warning(clonedWarning)
                .build()
            )
            .sorted(Comparator.comparing(WarningLocalityNames::getPosition))
            .collect(ImmutableList.toImmutableList());
    }

    private ImmutableSortedSet<GeneralArea> cloneGeneralAreas(Warning warning, Serie newSerie) {
        AtomicInteger generalAreaPosition = new AtomicInteger(1);
        return warning.getGeneralAreas().stream()
            .filter(generalArea -> newSerie.getSubAreas().stream()
                .map(SubArea::getId)
                .anyMatch(generalAreaId -> generalAreaId.equals(generalArea.getSubArea().getId())))
            .map(generalArea -> {
                GeneralArea clonedGeneralArea = new GeneralArea();
                clonedGeneralArea.setPosition(generalAreaPosition.getAndIncrement());
                clonedGeneralArea.setUrn(generalArea.getUrn());
                clonedGeneralArea.setName(generalArea.getSubArea().getName());
                clonedGeneralArea.setInternationalName(generalArea.getSubArea().getInternationalName());
                clonedGeneralArea.setSubArea(generalArea.getSubArea());
                return clonedGeneralArea;
            })
            .collect(toImmutableSortedSet(comparing(GeneralArea::getPosition)));
    }

    public String generateTelex(UUID id) {
        final Warning warning = repository.findById(id).orElseThrow(NotFoundException::new);
        if (PUBLISHED == warning.getStatus()) {
            return warning.getTelex();
        }
        return telexUtil.generateTelex(warning);
    }

    public String generateNavtex(UUID id) {
        final Warning warning = repository.findById(id).orElseThrow(NotFoundException::new);
        if (warning.getStatus() != PUBLISHED) {
            throw new BadRequestException("Current warning status is invalid. Current: " + warning.getStatus() + " -> expected: " + PUBLISHED);
        }
        return navtexUtil.generateNavtex(warning);
    }

    public List<MaritimeEntityConcernedDto> getAllWarningEntities(String username) {
        final Optional<ServiceUser> serviceUser = serviceUserRepository.findByEmail(username);
        if (serviceUser.isEmpty()) {
            throw new ForbiddenException();
        }
        return Stream.concat(
                repository.getAllWarningEntities().stream(),
                maritimeEntityRepository.findObservedEntities(serviceUser.get().getService().getId()).stream().map(MaritimeEntityConcernedDto::new)
            )
            .distinct()
            .collect(toList());
    }

    public List<MaritimeEntityConcernedDto> getAllEntitiesManagedOrObservedByService(String username) {
        final ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);

        return maritimeEntityRepository.findEntitiesManagedOrObservedByService(serviceUser.getService().getId()).stream()
            .map(MaritimeEntityConcernedDto::new)
            .collect(toList());
    }

    public List<MaritimeEntityConcernedDto> getLinkedEntities(String username, UUID serieId, boolean isFromAvurnavNoticeTable) {

        final Serie serie = serieService.findById(serieId);

        if (serie == null) {
            throw new NotFoundException();
        }

        if (!isFromAvurnavNoticeTable) {
            checkAndGetProUser(username, serie);
        }

        // Check if service can produce warnings
        if (!serie.getMaritimeService().isMaritimeNoticeProducer()) {
            throw new ForbiddenException("Service cannot produce warning");
        }

        final UUID serieServiceId = serie.getMaritimeService().getId();

        return maritimeEntityRepository.findEntitiesManagedOrObservedByService(serieServiceId).stream()
            .map(MaritimeEntityConcernedDto::new).collect(toList());
    }

    public List<FolderConcernedDto> getAllWarningFolders() {
        return repository.getAllWarningFolders();
    }

    @Transactional
    public void setReviewed(UUID id, String username) {
        final Warning warning = repository.findById(id).orElseThrow(NotFoundException::new);
        checkAndGetProUser(username, warning.getSerie());

        if (warning.getStatus() != PUBLISHED) {
            throw new BadRequestException("Current warning status is invalid");
        }

        warning.setForceToReview(false);
        repository.save(warning);
    }

    public Set<AttachmentDownloadDto> findAvailableAttachmentsToLinkAsWarningPublicReference(UUID warningId, String username) {
        Warning warning = repository.findById(warningId).orElseThrow(NotFoundException::new);

        checkIfServiceUserHasNoRequiredRoleOnSerieService(warning.getSerie(), username);

        return warning.getAttachments().stream()
            .map(warningAttachment -> new AttachmentDownloadDto(warningAttachment.getAttachment()))
            .collect(toSet());
    }

    public Set<WarningMaritimeEntityReferenceDto> findAvailableMaritimeEntitiesReferencesToLinkAsWarningPublicReference(UUID warningId, String username) {
        Warning warning = repository.findById(warningId).orElseThrow(NotFoundException::new);

        checkIfServiceUserHasNoRequiredRoleOnSerieService(warning.getSerie(), username);

        Set<WarningMaritimeEntityReferenceDto> availableEntitiesReferencesToLinkAsWarningReference = newHashSet();

        warning.getMaritimeEntities().forEach(maritimeEntity -> {
                Set<MaritimeEntityReference> availableMaritimeEntityReferences = maritimeEntity.getReferences().stream()
                    .filter(MaritimeEntityReference::isPublic)
                    .collect(toSet());

                availableEntitiesReferencesToLinkAsWarningReference.addAll(availableMaritimeEntityReferences.stream()
                    .map(maritimeEntityReference ->
                        new WarningMaritimeEntityReferenceDto(
                            maritimeEntity.getName(),
                            maritimeEntity.getInternationalName(),
                            maritimeEntityReference.asDto()))
                    .collect(toSet()));
            }
        );

        return availableEntitiesReferencesToLinkAsWarningReference;
    }

    public Set<WarningReferenceAsPublicReferenceDto> findAvailableWarningsReferencesToLinkAsWarningPublicReference(UUID warningId, String username) {
        Warning warning = repository.findById(warningId).orElseThrow(NotFoundException::new);

        checkIfServiceUserHasNoRequiredRoleOnSerieService(warning.getSerie(), username);

        Set<WarningReferenceAsPublicReferenceDto> availableWarningsReferencesToLinkAsWarningReference = newHashSet();
        warning.getWarningReferences().forEach(warningReference ->
            availableWarningsReferencesToLinkAsWarningReference.addAll(warningReference.getWarnings().stream()
                .map(warningToAdd -> new WarningReferenceAsPublicReferenceDto(warningToAdd, warningReference.getCategory()))
                .collect(toSet())
            )
        );

        return availableWarningsReferencesToLinkAsWarningReference;
    }

    private void checkIfServiceUserHasNoRequiredRoleOnSerieService(Serie serie, String username) {
        ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);

        MaritimeService service = serie.getMaritimeService();

        boolean serviceUserHasNoRequiredRoleOnSerieService = checkServiceUserHasNoRequiredRoleOnSerieService(
            serviceUser, service, SERVICE_OPERATOR, SERVICE_ADMIN
        );

        if (serviceUserHasNoRequiredRoleOnSerieService) {
            throw new ForbiddenException();
        }
    }

    public Set<UUID> getWarningIdsWhichCanBeCancelled(UUID warningId, String username) {
        Warning warning = repository.findById(warningId).orElseThrow(NotFoundException::new);

        checkIfServiceUserHasNoRequiredRoleOnSerieService(warning.getSerie(), username);

        List<UUID> warningIdsWhichCouldBeCancelled = getReferencesByCategory(warning, CANCELLATION);

        List<UUID> cancelledWarningIds = repository.getCancelledWarningIdsBySerie(
            warningIdsWhichCouldBeCancelled,
            warning.getSerie()
        );

        return warningIdsWhichCouldBeCancelled.stream()
            .filter(uuid -> !cancelledWarningIds.contains(uuid))
            .collect(toSet());
    }
}
