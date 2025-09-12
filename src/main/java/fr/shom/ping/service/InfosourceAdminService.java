package fr.shom.ping.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import fr.shom.ping.PingApiTranslationsRegistry;
import fr.shom.ping.exception.BadRequestException;
import fr.shom.ping.exception.ForbiddenException;
import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.Attachment;
import fr.shom.ping.model.Feature;
import fr.shom.ping.model.FeaturePart;
import fr.shom.ping.model.FixedDateRange;
import fr.shom.ping.model.Georegulation;
import fr.shom.ping.model.GeoregulationNumber;
import fr.shom.ping.model.GeoregulationPart;
import fr.shom.ping.model.GeoregulationPartTypeEntity;
import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.InfosourcePart;
import fr.shom.ping.model.InfosourceRecipient;
import fr.shom.ping.model.LocalizedText;
import fr.shom.ping.model.MaritimeEntity;
import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.Serie;
import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.ServiceUserRole;
import fr.shom.ping.model.Survey;
import fr.shom.ping.model.Tag;
import fr.shom.ping.model.User;
import fr.shom.ping.model.Warning;
import fr.shom.ping.model.WarningGeoregulation;
import fr.shom.ping.model.dto.FeatureDto;
import fr.shom.ping.model.dto.InfosourceActivityForecastCreateWithServiceUserDto;
import fr.shom.ping.model.dto.InfosourceActivityForecastDetailWithServiceUserDto;
import fr.shom.ping.model.dto.InfosourceAdminDetailDto;
import fr.shom.ping.model.dto.InfosourceAdminTableDto;
import fr.shom.ping.model.dto.InfosourceBathyAdminDetailDto;
import fr.shom.ping.model.dto.InfosourceBathyDangerReportCreateWithServiceUserDto;
import fr.shom.ping.model.dto.InfosourceBathyDangerReportDetailWithServiceUserDto;
import fr.shom.ping.model.dto.InfosourceCreateReport;
import fr.shom.ping.model.dto.InfosourceFutureUpdatesCreateWithServiceUserDto;
import fr.shom.ping.model.dto.InfosourceFutureUpdatesDetailWithServiceUserDto;
import fr.shom.ping.model.dto.InfosourceHazardReportCreateWithServiceUserDto;
import fr.shom.ping.model.dto.InfosourceHazardReportDetailWithServiceUserDto;
import fr.shom.ping.model.dto.InfosourceMapAnomalyCreateWithServiceUserDto;
import fr.shom.ping.model.dto.InfosourceMapAnomalyDetailWithServiceUserDto;
import fr.shom.ping.model.dto.InfosourceObjectDateRangeDto;
import fr.shom.ping.model.dto.InfosourceOtherReportCreateWithServiceUserDto;
import fr.shom.ping.model.dto.InfosourceOtherReportDetailWithServiceUserDto;
import fr.shom.ping.model.dto.InfosourcePublicationAnomalyCreateWithServiceUserDto;
import fr.shom.ping.model.dto.InfosourcePublicationAnomalyReportDetailWithServiceUserDto;
import fr.shom.ping.model.dto.InfosourceReferenceDto;
import fr.shom.ping.model.dto.InfosourceReportAbstractCreateDto;
import fr.shom.ping.model.dto.InfosourceServiceStatusServiceUserDto;
import fr.shom.ping.model.dto.InfosourceSurveyTransmissionAdminDetailDto;
import fr.shom.ping.model.dto.InfosourceToDisplayOnCreationMapDto;
import fr.shom.ping.model.dto.MaritimeEntityConcernedDto;
import fr.shom.ping.model.dto.MaritimeServiceInfosourceRecipientDto;
import fr.shom.ping.model.dto.UpdateInfosourceCommentDto;
import fr.shom.ping.model.dto.UpdateInfosourceServiceStatusDto;
import fr.shom.ping.model.enums.InfosourceNature;
import fr.shom.ping.model.enums.InfosourcePartObjectKeys;
import fr.shom.ping.model.enums.InfosourceState;
import fr.shom.ping.model.enums.LighthouseAndBeaconMessageDtoProperty;
import fr.shom.ping.model.enums.MaritimeServiceType;
import fr.shom.ping.model.enums.ServiceRole;
import fr.shom.ping.model.enums.WarningHazardTypeDetails;
import fr.shom.ping.model.enums.WarningHazardTypeGeneral;
import fr.shom.ping.model.enums.WarningStatus;
import fr.shom.ping.model.predicate.InfosourceAdminSearchCriteria;
import fr.shom.ping.properties.PingProperties;
import fr.shom.ping.repository.InfosourceActionRepository;
import fr.shom.ping.repository.InfosourceRepository;
import fr.shom.ping.repository.MaritimeEntityRepository;
import fr.shom.ping.repository.MaritimeServiceRepository;
import fr.shom.ping.repository.ServiceUserRepository;
import fr.shom.ping.repository.WarningRepository;
import fr.shom.ping.scheduling.SchedulerJobService;
import fr.shom.ping.utils.GeoregulationUtil;
import fr.shom.ping.utils.UserUtil;
import fr.shom.ping.utils.search.InfosourceAdminTableUtil;
import jakarta.persistence.EntityManager;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.locationtech.jts.geom.Geometry;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

import static com.google.common.collect.ImmutableSortedSet.toImmutableSortedSet;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newTreeSet;
import static fr.shom.ping.exception.ThrowingConsumerWrapper.throwingConsumerWrapper;
import static fr.shom.ping.model.enums.ActionType.INIT;
import static fr.shom.ping.model.enums.InfosourceNature.BATHY;
import static fr.shom.ping.model.enums.InfosourceNature.GEOREGULATION;
import static fr.shom.ping.model.enums.InfosourceNature.LIGHTHOUSE_AND_BEACON;
import static fr.shom.ping.model.enums.InfosourceNature.SURVEY_TRANSMISSION;
import static fr.shom.ping.model.enums.InfosourcePartObjectKeys.CHAPTER_OR_PAGE;
import static fr.shom.ping.model.enums.InfosourcePartObjectKeys.DATE_RANGES;
import static fr.shom.ping.model.enums.InfosourcePartObjectKeys.EDITION_NUMBER;
import static fr.shom.ping.model.enums.InfosourcePartObjectKeys.GEOREGULATION_CALENDAR_PERIODS;
import static fr.shom.ping.model.enums.InfosourcePartObjectKeys.GEOREGULATION_PART_DETAILS;
import static fr.shom.ping.model.enums.InfosourcePartObjectKeys.GEOREGULATION_PART_INFORMATION;
import static fr.shom.ping.model.enums.InfosourcePartObjectKeys.GEOREGULATION_PART_INTERNATIONAL_INFORMATION;
import static fr.shom.ping.model.enums.InfosourcePartObjectKeys.GEOREGULATION_PART_NUMBER;
import static fr.shom.ping.model.enums.InfosourcePartObjectKeys.GEOREGULATION_PART_TYPES;
import static fr.shom.ping.model.enums.InfosourcePartObjectKeys.GEOREGULATION_SEASONAL_PERIODS;
import static fr.shom.ping.model.enums.InfosourcePartObjectKeys.ISSUE_REFERENCE;
import static fr.shom.ping.model.enums.InfosourcePartObjectKeys.LAST_PATCH_NUMBER;
import static fr.shom.ping.model.enums.InfosourcePartObjectKeys.MAP_NUMBER;
import static fr.shom.ping.model.enums.InfosourcePartObjectKeys.OBJECT_FUTURE_INFO;
import static fr.shom.ping.model.enums.InfosourcePartObjectKeys.OBJECT_NAME;
import static fr.shom.ping.model.enums.InfosourcePartObjectKeys.UP_TO_DATE_AT;
import static fr.shom.ping.model.enums.InfosourceStatus.DRAFT;
import static fr.shom.ping.model.enums.InfosourceStatus.OPENED;
import static fr.shom.ping.model.enums.InfosourceStatus.REJECTED;
import static fr.shom.ping.model.enums.InfosourceStatus.SENT;
import static fr.shom.ping.model.enums.ServiceNetwork.GOVERNMENT_SERVICE;
import static fr.shom.ping.model.enums.ServiceRole.SERVICE_ADMIN;
import static fr.shom.ping.model.enums.ServiceRole.SERVICE_OPERATOR;
import static fr.shom.ping.model.enums.ServiceRole.SERVICE_VISITOR;
import static fr.shom.ping.model.enums.UserType.PRO;
import static fr.shom.ping.model.enums.WarningHazardTypeGeneral.AIDS_TO_NAVIGATION;
import static fr.shom.ping.model.enums.WarningHazardTypeGeneral.HAZARD_OTHER;
import static fr.shom.ping.model.enums.WarningHazardTypeGeneral.NEWLY_DISCOVERED_DANGERS;
import static fr.shom.ping.model.enums.WarningHazardTypeGeneral.ROUTING_INFORMATION;
import static fr.shom.ping.model.enums.WarningHazardTypeGeneral.WORKS;
import static fr.shom.ping.service.SurveyExternalAnalysisService.getLastProcessingResult;
import static fr.shom.ping.utils.InfosourceUtil.groupInfosourceToDisplayOnCreationMapTupleByInfosourceId;
import static fr.shom.ping.validation.ValidTimezoneValidator.convertToUtcHoursAndMinutes;
import static java.time.Instant.now;
import static java.time.ZoneOffset.UTC;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static java.util.Locale.ENGLISH;
import static java.util.Locale.FRANCE;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
public class InfosourceAdminService extends AbstractInfosourceService {

    private static final String DESCRIPTION_LINE_SEPARATOR = "\n";

    private final ServiceUserRepository serviceUserRepository;
    private static final String FEATURE_PART_EXCEPTION = "Information is not filled for at least one feature part";
    private final SerieService serieService;

    private final TagService tagService;

    private final WarningRepository warningRepository;

    private final MaritimeEntityRepository maritimeEntityRepository;

    private final InfosourceActionRepository infosourceActionRepository;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private PingApiTranslationsRegistry translationsRegistry;

    public InfosourceAdminService(EntityManager entityManager, InfosourceRepository repository, InfosourceActionRepository infosourceActionRepository,
        AttachmentService attachmentService, InfosourceRoutingService routingService, ServiceUserRepository serviceUserRepository,
        MaritimeServiceRepository maritimeServiceRepository, InfosourceRecipientService recipientService, SerieService serieService,
        TagService tagService, WarningRepository warningRepository, MaritimeEntityRepository maritimeEntityRepository,
        NamedParameterJdbcTemplate namedParameterJdbcTemplate, PingApiTranslationsRegistry translationsRegistry, SchedulerJobService schedulerJobService,
        FolderService folderService, PingProperties pingProperties) {
        super(entityManager, repository, attachmentService, routingService, maritimeServiceRepository, recipientService, schedulerJobService, folderService, pingProperties);
        this.serviceUserRepository = serviceUserRepository;
        this.serieService = serieService;
        this.tagService = tagService;
        this.warningRepository = warningRepository;
        this.maritimeEntityRepository = maritimeEntityRepository;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.translationsRegistry = translationsRegistry;
        this.infosourceActionRepository = infosourceActionRepository;
    }

    public List<InfosourceToDisplayOnCreationMapDto> findAllToDisplayOnCreationMap(UUID infosourceId, String username) {
        Optional<ServiceUser> optionalServiceUser = serviceUserRepository.findByEmail(username);
        if (optionalServiceUser.isEmpty()) {
            throw new ForbiddenException();
        }

        ServiceUser serviceUser = optionalServiceUser.get();
        if (infosourceId == null) {
            return groupInfosourceToDisplayOnCreationMapTupleByInfosourceId(
                repository.findAllToDisplayOnCreationMapByServiceUserIdOrService(serviceUser.getId(), serviceUser.getService())
            );
        }

        return groupInfosourceToDisplayOnCreationMapTupleByInfosourceId(
            repository.findAllToDisplayOnCreationMapByServiceUserIdOrServiceAndExcludedInfosource(infosourceId, serviceUser.getId(),
                serviceUser.getService())
        );

    }

    @Transactional
    public UUID createHazardReport(InfosourceHazardReportCreateWithServiceUserDto dto, BindingResult bindingResult, String username) throws ServiceException {
        return createReport(dto, bindingResult, username);
    }

    @Transactional
    public UUID createOtherReport(InfosourceOtherReportCreateWithServiceUserDto dto, BindingResult bindingResult, String username) throws ServiceException {
        return createReport(dto, bindingResult, username);
    }

    @Transactional
    public UUID createMapAnomaly(InfosourceMapAnomalyCreateWithServiceUserDto dto, BindingResult bindingResult, String username) throws ServiceException {
        return createReport(dto, bindingResult, username);
    }

    @Transactional
    public UUID createFutureUpdates(InfosourceFutureUpdatesCreateWithServiceUserDto dto, BindingResult bindingResult, String username) throws ServiceException {
        return createReport(dto, bindingResult, username);
    }

    @Transactional
    public UUID createPublicationAnomaly(InfosourcePublicationAnomalyCreateWithServiceUserDto dto,
        BindingResult bindingResult, String username) throws ServiceException {
        return createReport(dto, bindingResult, username);
    }

    @Transactional
    public UUID createReport(InfosourceCreateReport dto, BindingResult bindingResult, String username) throws ServiceException {
        ServiceUser serviceUser = getServiceUser(bindingResult, username);

        checkIfLocalisationIsEmpty(dto.getLocalisation());

        MaritimeService service = maritimeServiceRepository.findById(dto.getServiceId()).orElseThrow(NotFoundException::new);
        List<Attachment> attachments = attachmentService.findAllByIdIn(dto.getAttachmentIds());
        List<MaritimeEntity> entities = maritimeEntityRepository.findAllById(ofNullable(dto.getConcernedMaritimeEntityIds()).orElse(emptyList()));

        Infosource infosource = dto.toInfosource(attachments, serviceUser, service, entities);
        final Infosource savedInfosource = repository.save(infosource);

        // schedule IS draft deletion
        schedulerJobService.scheduleAutoDeleteDraftInfosourceJob(savedInfosource);

        return savedInfosource.getId();
    }

    public ServiceUser getServiceUser(BindingResult bindingResult, String username) throws ServiceException {
        final ServiceUser serviceUser = getServiceUser(username);

        if (bindingResult.hasErrors()) {
            throw new ServiceException();
        }
        return serviceUser;
    }

    public ServiceUser getServiceUser(String username) {
        return serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);
    }

    public InfosourceReportAbstractCreateDto findById(UUID id, String username) {
        ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);

        Infosource infosource = getInfosource(id, serviceUser);

        switch (infosource.getNature()) {
            case HAZARD_REPORT:
                return new InfosourceHazardReportDetailWithServiceUserDto(infosource);
            case OTHER_REPORT:
                return new InfosourceOtherReportDetailWithServiceUserDto(infosource);
            case MAP_ANOMALY:
                return new InfosourceMapAnomalyDetailWithServiceUserDto(infosource);
            case PUBLICATION_ANOMALY:
                return new InfosourcePublicationAnomalyReportDetailWithServiceUserDto(infosource);
            case FUTURE_INFORMATION:
                return new InfosourceFutureUpdatesDetailWithServiceUserDto(infosource);
            case BATHY:
                return new InfosourceBathyDangerReportDetailWithServiceUserDto(infosource, getLastProcessingResult(infosource.getSurvey()).getDetections());
            default:
                throw new NotFoundException();
        }
    }

    public InfosourceActivityForecastDetailWithServiceUserDto findActivityForecastInfosourceById(UUID id, String username) {
        ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);
        Infosource infosource = getInfosource(id, serviceUser);
        return new InfosourceActivityForecastDetailWithServiceUserDto(infosource);
    }

    @Transactional
    @SuppressWarnings("java:S103")
    public UUID updateComment(UUID id, UpdateInfosourceCommentDto dto, BindingResult bindingResult, String username) throws ServiceException {
        ServiceUser serviceUser = getServiceUser(bindingResult, username);
        Infosource infosource = getInfosourceToComment(id, serviceUser);
        infosource.setComment(dto.getComment());
        return repository.save(infosource).getId();
    }

    @Transactional
    @SuppressWarnings("java:S103")
    public UUID updateHazardReport(UUID id, InfosourceHazardReportCreateWithServiceUserDto dto, BindingResult bindingResult, String username) throws
        ServiceException {
        return updateReport(id, dto, bindingResult, username);
    }

    @Transactional
    @SuppressWarnings("java:S103")
    public UUID updateOtherReport(UUID id, InfosourceOtherReportCreateWithServiceUserDto dto, BindingResult bindingResult, String username) throws
        ServiceException {
        return updateReport(id, dto, bindingResult, username);
    }

    @Transactional
    @SuppressWarnings("java:S103")
    public UUID updateMapAnomaly(UUID id, InfosourceMapAnomalyCreateWithServiceUserDto dto, BindingResult bindingResult, String username) throws
        ServiceException {
        return updateReport(id, dto, bindingResult, username);
    }

    @Transactional
    @SuppressWarnings("java:S103")
    public UUID updateFutureUpdates(UUID id, InfosourceFutureUpdatesCreateWithServiceUserDto dto, BindingResult bindingResult, String username) throws
        ServiceException {
        return updateReport(id, dto, bindingResult, username);
    }

    @Transactional
    public UUID updatePublicationAnomaly(UUID id, InfosourcePublicationAnomalyCreateWithServiceUserDto dto, BindingResult bindingResult,
        String username) throws ServiceException {
        return updateReport(id, dto, bindingResult, username);
    }

    @Transactional
    public UUID updateActivityForecast(UUID id, InfosourceActivityForecastDetailWithServiceUserDto dto, BindingResult bindingResult,
        String username) throws ServiceException {
        ServiceUser serviceUser = getServiceUser(bindingResult, username);
        MaritimeService service = maritimeServiceRepository.findById(dto.getServiceId()).orElseThrow(NotFoundException::new);
        List<Attachment> attachments = attachmentService.findAllByIdIn(dto.getAttachmentIds());
        List<MaritimeEntity> entities = maritimeEntityRepository.findAllById(ofNullable(dto.getConcernedMaritimeEntityIds()).orElse(emptyList()));
        Infosource infosourceToUpdate = dto.toInfosource(attachments, serviceUser, service, entities);
        Infosource infosource = getInfosource(id, serviceUser);

        infosource.setComment(infosourceToUpdate.getComment());
        infosource.setTimezone(infosourceToUpdate.getTimezone());
        infosource.setIssuingService(infosourceToUpdate.getIssuingService());

        // For each part, check if at least information is filled
        boolean atLeastInformationIsFilledForEachPart = dto.getParts().stream()
            .allMatch(part -> !part.getInformation().isEmpty());

        if (!atLeastInformationIsFilledForEachPart) {
            throw new BadRequestException(FEATURE_PART_EXCEPTION);
        }
        SortedSet<InfosourcePart> featureParts = infosourceToUpdate.getInfosourceParts();
        featureParts.forEach(featurePart -> featurePart.setInfosource(infosource));
        infosource.getInfosourceParts().clear();
        infosource.getInfosourceParts().addAll(infosourceToUpdate.getInfosourceParts());

        final List<UUID> infosourceUpdatedAttachmentsIds = infosourceToUpdate.getAttachments().stream().map(Attachment::getId).collect(toList());
        deleteUnusedAttachments(infosource, infosourceUpdatedAttachmentsIds);
        Set<Attachment> attachmentsUpdated = newHashSet(infosourceToUpdate.getAttachments());
        infosource.getAttachments().clear();
        infosource.getAttachments().addAll(attachmentsUpdated);

        infosource.getEntities().clear();
        infosource.getEntities().addAll(newHashSet(infosourceToUpdate.getEntities()));

        infosource.setLastUpdate(now());

        final Infosource savedInfosource = repository.save(infosource);

        // remove scheduling of deletion && schedule new deletion with lastUpdate + 30 days
        schedulerJobService.scheduleAutoDeleteDraftInfosourceJob(savedInfosource);

        return savedInfosource.getId();
    }

    @Transactional
    public UUID updateReport(UUID id, InfosourceCreateReport dto, BindingResult bindingResult, String username) throws ServiceException {
        ServiceUser serviceUser = getServiceUser(bindingResult, username);
        MaritimeService service = maritimeServiceRepository.findById(dto.getServiceId()).orElseThrow(NotFoundException::new);
        List<Attachment> attachments = attachmentService.findAllByIdIn(dto.getAttachmentIds());
        List<MaritimeEntity> entities = maritimeEntityRepository.findAllById(ofNullable(dto.getConcernedMaritimeEntityIds()).orElse(emptyList()));
        Infosource infosourceToUpdate = dto.toInfosource(attachments, serviceUser, service, entities);
        return updateInfosource(id, infosourceToUpdate, serviceUser);
    }

    private UUID updateInfosource(UUID id, Infosource infosourceToUpdate, ServiceUser serviceUser) {
        Infosource infosource = getInfosource(id, serviceUser);
        infosource.getInfosourceParts().first().setObject(infosourceToUpdate.getInfosourceParts().first().getObject());
        infosource.getInfosourceParts().first().setDescription(infosourceToUpdate.getInfosourceParts().first().getDescription());
        infosource.getInfosourceParts().first().setLocalisation(infosourceToUpdate.getInfosourceParts().first().getLocalisation());
        infosource.setComment(infosourceToUpdate.getComment());
        infosource.getInfosourceParts().first().setInternationalDescription(infosourceToUpdate.getInfosourceParts().first().getInternationalDescription());
        infosource.setIssuingService(infosourceToUpdate.getIssuingService());
        infosource.setTimezone(infosourceToUpdate.getTimezone());
        infosource.setConfidentialityInformation(infosourceToUpdate.getConfidentialityInformation());
        infosource.setStartDate(infosourceToUpdate.getStartDate());
        infosource.setEndDate(infosourceToUpdate.getEndDate());

        // update attachments
        final List<UUID> infosourceUpdatedAttachmentsIds = infosourceToUpdate.getAttachments().stream().map(Attachment::getId).collect(toList());
        deleteUnusedAttachments(infosource, infosourceUpdatedAttachmentsIds);
        Set<Attachment> attachments = newHashSet(infosourceToUpdate.getAttachments());
        infosource.getAttachments().clear();
        infosource.getAttachments().addAll(attachments);

        infosource.getEntities().clear();
        infosource.getEntities().addAll(newHashSet(infosourceToUpdate.getEntities()));

        infosource.setLastUpdate(now());

        final Infosource savedInfosource = repository.save(infosource);

        // remove scheduling of deletion && schedule new deletion with lastUpdate + 30 days
        schedulerJobService.scheduleAutoDeleteDraftInfosourceJob(savedInfosource);

        return savedInfosource.getId();
    }

    /**
     * delete files from disk using AttachmentService.delete()
     *
     * @param infosource            the warning from db
     * @param updatedAttachmentList attachment list to update
     */
    public void deleteUnusedAttachments(Infosource infosource, List<UUID> updatedAttachmentList) {
        final List<UUID> formerAttachmentList = infosource.getAttachments().stream().map(Attachment::getId).collect(toList());
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

    private Infosource getInfosource(UUID id, ServiceUser serviceUser) {
        Infosource infosource = repository.findById(id).orElseThrow(ForbiddenException::new);

        if (!serviceUser.getServiceIdsWithOperatorOrAdminRole().contains(infosource.getIssuingService().getId())) {
            throw new ForbiddenException();
        }

        return infosource;
    }

    private Infosource getInfosourceToComment(UUID id, ServiceUser serviceUser) {
        Infosource infosource = repository.findById(id).orElseThrow(ForbiddenException::new);

        // check if user is link to issuer service, or if one of IS recipient services is link to user
        boolean canBeUpdated =
            (infosource.getIssuingService() != null && serviceUser.getServiceIdsWithOperatorOrAdminRole().contains(infosource.getIssuingService().getId()))
            || serviceUser.getServiceIdsWithOperatorOrAdminRole().stream()
                .anyMatch(serviceId -> infosource.getRecipients().stream()
                    .map(InfosourceRecipient::getService)
                    .map(MaritimeService::getId)
                    .toList()
                    .contains(serviceId));

        if (!canBeUpdated) {
            throw new ForbiddenException();
        }

        return infosource;
    }

    public PageImpl<InfosourceAdminTableDto> findAllBySearchCriteria(InfosourceAdminSearchCriteria searchCriteria, Pageable pageable, String username,
        String lang) {
        ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);
        return InfosourceAdminTableUtil.find(entityManager, searchCriteria, pageable, serviceUser.getId(), lang);
    }

    @Transactional
    public InfosourceAdminDetailDto findByIdForDetailsView(UUID id, String username) {
        ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);
        Infosource infosource = repository.findById(id).orElseThrow(NotFoundException::new);
        final List<MaritimeService> servicesBri = maritimeServiceRepository.findAllByTypeIn(MaritimeServiceType.BRI);

        final boolean isBri = UserUtil.isBri(serviceUser, servicesBri);

        setStatusToOpenAndAddReadActionIfNeeded(serviceUser, infosource);
        InfosourceAdminDetailDto infosourceAdminDetailDto;

        switch (infosource.getNature()) {
            case SURVEY_TRANSMISSION:
                infosourceAdminDetailDto = new InfosourceSurveyTransmissionAdminDetailDto().fromInfosource(infosource, serviceUser, isBri);
                break;

            case BATHY:
                infosourceAdminDetailDto = new InfosourceBathyAdminDetailDto().fromInfosource(infosource, serviceUser, isBri);
                break;

            default:
                infosourceAdminDetailDto = new InfosourceAdminDetailDto().fromInfosource(infosource, serviceUser);
                break;
        }

        if (infosourceAdminDetailDto.getStatus() == REJECTED) {
            infosourceActionRepository.findRejectComment(id).ifPresent(infosourceAdminDetailDto::setRejectComment);
        }

        infosourceAdminDetailDto.setCanBeAddedToFolder(
            infosource.getStatus() != DRAFT && maritimeServiceRepository.hasAnyFolderManagerByIds(
                serviceUser.getRoles().stream()
                    .filter(serviceUserRole -> asList(SERVICE_OPERATOR, SERVICE_ADMIN).contains(serviceUserRole.getServiceRole()))
                    .map(ServiceUserRole::getServiceId)
                    .collect(toList())
            )
        );

        return infosourceAdminDetailDto;
    }

    @Transactional
    public void setStatusToOpenAndAddReadActionIfNeeded(ServiceUser serviceUser, Infosource infosource) {
        if (asList(DRAFT, REJECTED).contains(infosource.getStatus())) {
            return;
        }

        List<MaritimeService> recipientServices = infosource.getRecipients().stream()
            .filter(recipient -> recipient.getStatus() == INIT)
            .map(InfosourceRecipient::getService)
            .collect(toList());

        List<UUID> serviceUserServiceIdsWithRequiredRoles = serviceUser.getRoles().stream()
            .filter(serviceUserRole -> asList(SERVICE_OPERATOR, SERVICE_ADMIN).contains(serviceUserRole.getServiceRole()))
            .map(ServiceUserRole::getServiceId)
            .collect(toList());

        List<MaritimeService> recipientServicesWithRequiredRoles = recipientServices.stream()
            .filter(recipientService -> serviceUserServiceIdsWithRequiredRoles.contains(recipientService.getId()))
            .collect(toList());

        if (recipientServicesWithRequiredRoles.isEmpty()) {
            return;
        }

        if (infosource.getStatus() == SENT) {
            infosource.setStatus(OPENED);
            repository.save(infosource);
        }

        recipientService.read(infosource, serviceUser, recipientServicesWithRequiredRoles);
    }

    @Transactional
    public void send(UUID infosourceId, String username, Set<String> maritimeServiceIds) {
        final ServiceUser serviceUser = getServiceUser(username);

        final Infosource infosource = checkServiceUserRoleAndGetInfosource(serviceUser, infosourceId);

        List<MaritimeService> recipients = maritimeServiceRepository.findAllById(
            maritimeServiceIds.stream().map(UUID::fromString).collect(toSet())
        );

        if (recipients.size() != maritimeServiceIds.size()) {
            throw new BadRequestException("Failed to find at least one service from given maritime service id list");
        }

        final Infosource savedInfosource = sendInfosource(infosource, serviceUser);
        recipientService.send(recipients, savedInfosource, serviceUser.getId(), PRO, true);

        // cancel deletion job
        schedulerJobService.removeScheduledAutoDeleteDraftInfosource(savedInfosource);
    }

    @Override
    protected long countReportedInfosourcesByUserWithinYear(User user, int year) {
        return repository.countReportedInfosourcesByServiceUserWithinYear(user.getId(), year);
    }

    @Override
    protected void assignUserAsPublisher(User user, Infosource infosource) {
        final ServiceUser serviceUser = getServiceUser(user.getEmail());
        infosource.setIssuingServiceUser(serviceUser);
    }

    @Override
    @Transactional
    public void delete(UUID id, String username) {
        final ServiceUser serviceUser = getServiceUser(username);
        final Infosource infosource = checkServiceUserRoleAndGetInfosource(serviceUser, id);
        if (infosource.getStatus() != DRAFT) {
            throw new BadRequestException("Current infosource status is invalid");
        }

        deleteAttachments(infosource);

        // cancel deletion job
        schedulerJobService.removeScheduledAutoDeleteDraftInfosource(infosource);
        repository.delete(infosource);
    }

    private Infosource checkServiceUserRoleAndGetInfosource(ServiceUser serviceUser, UUID infosourceId) {
        List<UUID> serviceIds = serviceUser.getRoles().stream()
            .filter(serviceUserRole -> asList(SERVICE_OPERATOR, SERVICE_ADMIN).contains(serviceUserRole.getServiceRole()))
            .map(ServiceUserRole::getServiceId)
            .collect(toList());

        return repository.findByIdAndIssuingServiceIdIn(infosourceId, serviceIds).orElseThrow(NotFoundException::new);
    }

    public Set<MaritimeServiceInfosourceRecipientDto> getRoutingServices(UUID infosourceId, String username) {
        final ServiceUser serviceUser = getServiceUser(username);

        final Infosource infosource = checkServiceUserRoleAndGetInfosource(serviceUser, infosourceId);

        Set<MaritimeServiceInfosourceRecipientDto> recipients;
        try {
            recipients = routingService.getRecipients(infosource, true);
        } catch (ServiceException e) {
            throw new BadRequestException(e.getMessage());
        }
        return recipients;
    }

    @Transactional
    public void reject(UUID id, String optionalComment, String username) {
        ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);
        if (serviceUser.getService().getServiceNetwork() != GOVERNMENT_SERVICE) {
            throw new ForbiddenException();
        }

        boolean hasBeenRejected = repository.reject(id, now()) == 1;
        if (!hasBeenRejected) {
            throw new BadRequestException();
        }

        Infosource infosource = repository.findById(id).orElseThrow(NotFoundException::new);
        recipientService.archive(infosource, serviceUser, optionalComment);
    }

    public List<InfosourceServiceStatusServiceUserDto> findServiceStatuses(UUID id) {
        return recipientService.findServiceStatusesForServiceUser(id);
    }

    @Transactional
    public void updateServiceStatus(UUID id, UpdateInfosourceServiceStatusDto dto, String username) {
        ServiceUser serviceUser = getServiceUser(username);
        Infosource infosource = repository.findByIdAndRecipientIds(id, singletonList(dto.getServiceId())).orElseThrow(ForbiddenException::new);

        if (infosource.getStatus() == DRAFT) {
            throw new BadRequestException("Infosource is in DRAFT status");
        }

        MaritimeService service = maritimeServiceRepository.findById(dto.getServiceId()).orElseThrow(NotFoundException::new);
        recipientService.updateStatus(infosource, serviceUser, service, dto.getActionType());
    }

    @Transactional
    public UUID createActivityForecast(InfosourceActivityForecastCreateWithServiceUserDto dto, BindingResult bindingResult, String username) throws
        ServiceException {
        final ServiceUser serviceUser = getServiceUser(bindingResult, username);
        final MaritimeService service = maritimeServiceRepository.findById(dto.getServiceId()).orElseThrow(NotFoundException::new);
        final List<Attachment> attachments = attachmentService.findAllByIdIn(dto.getAttachmentIds());
        final List<MaritimeEntity> entities = maritimeEntityRepository.findAllById(ofNullable(dto.getConcernedMaritimeEntityIds()).orElse(emptyList()));
        final Infosource infosource = dto.toInfosource(attachments, serviceUser, service, entities);
        final Infosource savedInfosource = repository.save(infosource);

        // schedule IS draft deletion
        schedulerJobService.scheduleAutoDeleteDraftInfosourceJob(savedInfosource);

        return savedInfosource.getId();
    }

    public Set<MaritimeServiceInfosourceRecipientDto> findTransferableServices(UUID id, String username) {
        ServiceUser serviceUser = getServiceUser(username);
        List<UUID> recipientIds = serviceUser.getServiceIdsWithOperatorOrAdminRole();
        Infosource infosource = repository.findByIdAndRecipientIds(id, recipientIds).orElseThrow(ForbiddenException::new);

        Set<MaritimeServiceInfosourceRecipientDto> transferableServices = infosource.getInfosourceParts().stream()
            .map(InfosourcePart::getLocalisation)
            .flatMap(Collection::stream)
            .map(Feature::getGeometry)
            .map(maritimeServiceRepository::findAllAsDtoByAreaOrEntityIntersectWith)
            .flatMap(Collection::stream)
            .collect(toSet());

        if (transferableServices.isEmpty()) {
            throw new BadRequestException("infosource-transfer.rejected");
        }

        List<UUID> currentRecipientServiceIds = recipientService.findRecipientServiceIdsByInfosource(infosource);
        transferableServices = transferableServices.stream()
            .filter(recipient -> !currentRecipientServiceIds.contains(recipient.getId()))
            .collect(toSet());

        if (transferableServices.isEmpty()) {
            throw new BadRequestException("infosource-transfer.rejected");
        }

        return transferableServices;
    }

    public void transfer(UUID id, List<UUID> serviceIdsToTransferTo, String username) {
        ServiceUser serviceUser = getServiceUser(username);
        List<UUID> recipientIds = serviceUser.getServiceIdsWithOperatorOrAdminRole();
        Infosource infosource = repository.findByIdAndRecipientIds(id, recipientIds).orElseThrow(ForbiddenException::new);

        if (infosource.getStatus() == DRAFT) {
            throw new BadRequestException("Infosource is in DRAFT status");
        }

        List<MaritimeService> transferableServices = infosource.getInfosourceParts().stream()
            .map(InfosourcePart::getLocalisation)
            .flatMap(Collection::stream)
            .map(Feature::getGeometry)
            .map(maritimeServiceRepository::findAllByAreaOrEntityIntersectWith)
            .flatMap(Collection::stream)
            .collect(toList());

        if (transferableServices.isEmpty()) {
            throw new BadRequestException("infosource-transfer.rejected");
        }

        boolean allServiceIdsToTransferToAreTransferable = transferableServices.stream()
            .map(MaritimeService::getId)
            .collect(toList())
            .containsAll(serviceIdsToTransferTo);

        if (!allServiceIdsToTransferToAreTransferable) {
            throw new BadRequestException("At least one service to transfer to is not suitable");
        }

        List<MaritimeService> servicesToTransferTo = serviceIdsToTransferTo.stream()
            .map(serviceIdToTransferTo -> transferableServices.stream()
                .filter(transferableService -> transferableService.getId().equals(serviceIdToTransferTo))
                .findFirst()
                .get())
            .collect(toList());

        List<UUID> currentRecipientServiceIds = recipientService.findRecipientServiceIdsByInfosource(infosource);
        servicesToTransferTo = servicesToTransferTo.stream()
            .filter(recipient -> !currentRecipientServiceIds.contains(recipient.getId()))
            .collect(toList());

        if (servicesToTransferTo.isEmpty()) {
            throw new BadRequestException("infosource-transfer.rejected");
        }

        recipientService.transfer(infosource, serviceUser, servicesToTransferTo);
    }

    public List<MaritimeServiceInfosourceRecipientDto> getAvailableRecipientsFromSentInfosources(UUID issuingServiceId, InfosourceState state) {
        switch (state) {
            case SENT:
                return maritimeServiceRepository.findAllRecipientsFromSentInfosourcesWithIssuingServiceId(issuingServiceId);
            case RECEIVED:
                return maritimeServiceRepository.findAllRecipientsFromSentInfosourcesWithIssuingServiceIdInRecipient(issuingServiceId);
            default:
                break;
        }
        return emptyList();
    }

    private boolean hasNoRoleOnInfosourceRecipientService(ServiceUser serviceUser, Infosource infosource) {
        return hasNoRequiredRoleOnInfosourceRecipientService(serviceUser, infosource, SERVICE_VISITOR, SERVICE_OPERATOR, SERVICE_ADMIN);
    }

    private boolean hasNoAdminNorOperatorRoleOnInfosourceRecipientService(ServiceUser serviceUser, Infosource infosource) {
        return hasNoRequiredRoleOnInfosourceRecipientService(serviceUser, infosource, SERVICE_OPERATOR, SERVICE_ADMIN);
    }

    private boolean hasNoRequiredRoleOnInfosourceRecipientService(ServiceUser serviceUser, Infosource infosource, ServiceRole... serviceRoles) {
        final List<UUID> recipientServiceIds = recipientService.findRecipientServiceIdsByInfosource(infosource);
        return serviceUser.getRoles().stream()
            .filter(serviceUserRole -> recipientServiceIds.contains(serviceUserRole.getServiceId()))
            .noneMatch(serviceUserRole -> asList(serviceRoles).contains(serviceUserRole.getServiceRole()));
    }

    private boolean hasNoRoleOnService(ServiceUser serviceUser, UUID serviceId) {
        return serviceUser.getRoles().stream()
            .filter(serviceUserRole -> asList(SERVICE_VISITOR, SERVICE_OPERATOR, SERVICE_ADMIN).contains(serviceUserRole.getServiceRole()))
            .map(ServiceUserRole::getServiceId)
            .noneMatch(uuid -> uuid.equals(serviceId));
    }

    @Transactional
    public UUID createWarning(UUID id, UUID serieId, boolean copyDataFromInfosource, String username) {
        final ServiceUser serviceUser = getServiceUser(username);

        final Serie chosenSerie = serieService.findByIdAndUserWithAdminOrOperatorRole(serieId, serviceUser.getId());

        if (chosenSerie == null) {
            throw new ForbiddenException();
        }

        // Check if service can produce warnings
        if (!chosenSerie.getMaritimeService().isMaritimeNoticeProducer()) {
            throw new ForbiddenException("Service cannot produce warning");
        }

        final Infosource infosource = repository.findById(id).orElseThrow(NotFoundException::new);

        if (infosource.getStatus() == DRAFT) {
            throw new BadRequestException("Infosource is in DRAFT status");
        }

        if (infosource.getNature() == SURVEY_TRANSMISSION) {
            throw new BadRequestException("Infosource whose nature is SURVEY_TRANSMISSION cannot be used to create a warning");
        }

        boolean serviceUserHasNoRequiredRoleOnRecipientService = hasNoAdminNorOperatorRoleOnInfosourceRecipientService(serviceUser, infosource);

        if (serviceUserHasNoRequiredRoleOnRecipientService) {
            throw new ForbiddenException();
        }

        if (infosource.getNature() == GEOREGULATION) {
            return createFromGeoregulation(chosenSerie, serviceUser, infosource, copyDataFromInfosource);
        }

        if (infosource.getNature() == BATHY) {
            checkBathyGeometries(
                infosource.getInfosourceParts().stream()
                    .findFirst().get().getLocalisation().stream()
                    .map(FeatureDto::new)
                    .collect(toSet())
            );
            return createFromInfosourceBathy(chosenSerie, serviceUser, infosource, copyDataFromInfosource);
        }

        final Warning warningToCreate = new Warning();
        warningToCreate.setSerie(chosenSerie);
        warningToCreate.setStatus(WarningStatus.DRAFT);

        final Instant now = now();
        warningToCreate.setCreationDate(now);
        warningToCreate.setLastUpdate(now);
        warningToCreate.setLastUpdateUser(serviceUser);
        warningToCreate.setNameOfSeries(chosenSerie.getName());
        warningToCreate.setProductionAgency(chosenSerie.getMaritimeService().getProductionAgency());
        warningToCreate.setWarningHazardTypeGeneral(getWarningHazardTypeGeneralFromInfosourceNature(infosource.getNature()));

        if (copyDataFromInfosource) {
            warningToCreate.setFeatureParts(copyFeatureParts(infosource, chosenSerie));
        }

        // add reference to infosource
        warningToCreate.getInfosources().add(infosource);
        final Warning savedWarning = warningRepository.save(warningToCreate);

        folderService.addWarningToInfosourceFolders(infosource.getId(), savedWarning);

        return savedWarning.getId();
    }

    private static WarningHazardTypeGeneral getWarningHazardTypeGeneralFromInfosourceNature(InfosourceNature infosourceNature) {
        switch (infosourceNature) {
            case HAZARD_REPORT:
            case LIGHTHOUSE_AND_BEACON:
                return AIDS_TO_NAVIGATION;
            case OTHER_REPORT:
                return HAZARD_OTHER;
            case ACTIVITY_FORECAST:
                return WORKS;
            case MAP_ANOMALY:
            case PUBLICATION_ANOMALY:
            case FUTURE_INFORMATION:
            default:
                return NEWLY_DISCOVERED_DANGERS;
        }
    }

    private ImmutableSortedSet<FeaturePart> copyFeatureParts(Infosource infosource, Serie serie) {
        return infosource.getInfosourceParts().stream()
            .map(infosourcePart -> {
                FeaturePart clonedFeaturePart = new FeaturePart();
                WarningHazardTypeDetails type = getWarningHazardTypeDetails(infosource.getObject());
                clonedFeaturePart.setPosition(infosourcePart.getPosition());
                clonedFeaturePart.setGeometries(new HashSet<>(infosourcePart.getLocalisation()));

                if (type != null) {
                    clonedFeaturePart.setInformation(createInformation(infosource.getObject(), infosourcePart, serie, infosource.getNature(), type));
                    clonedFeaturePart.setWarningHazardTypeDetails(newHashSet(type));
                } else {
                    clonedFeaturePart.setInformation(createInformation(infosource.getObject(), infosourcePart, serie, infosource.getNature(), null));
                }

                if (infosourcePart.getObject() != null) {
                    clonedFeaturePart.setFixedDateRanges(InfosourceObjectDateRangeDto.getDateRanges((List<Map<String, String>>) infosourcePart.getObject().get(DATE_RANGES.getKeyName()), infosource.getTimezone()));
                }
                return clonedFeaturePart;
            })
            .collect(toImmutableSortedSet(comparing(FeaturePart::getPosition)));
    }

    private Set<LocalizedText> createInformation(Map<String, Object> infosourceObject,
        InfosourcePart infosourcePart, Serie serie, InfosourceNature infosourceNature, WarningHazardTypeDetails type) {
        final List<String> supportedLanguages = Stream.of(serie.getMandatoryLanguages(), serie.getOptionalLanguages())
            .flatMap(Collection::stream)
            .collect(toList());
        final LinkedHashSet<LocalizedText> filteredInformationBySerieSupportedLanguages = new LinkedHashSet<>();
        supportedLanguages.forEach(language -> {
            String text = infosourcePart.getDescription();
            final boolean internationalLanguage = Locale.ENGLISH.getLanguage().equalsIgnoreCase(language);
            if (internationalLanguage && isNotBlank(infosourcePart.getInternationalDescription())) {
                text = infosourcePart.getInternationalDescription();
            }
            final String objectFields = generateObjectFieldsDescription(infosourceObject, infosourcePart.getObject(), infosourceNature, language, type);
            filteredInformationBySerieSupportedLanguages.add(LocalizedText.builder().language(language).text(objectFields + text).build());
        });
        return filteredInformationBySerieSupportedLanguages;
    }

    private String generateObjectFieldsDescription(Map<String, Object> infosourceObject, Map<String, Object> object,
        InfosourceNature infosourceNature, String language, WarningHazardTypeDetails type) {
        if (object == null || object.isEmpty()) {
            return "";
        }

        final StringBuilder formattedObjectFields = new StringBuilder();
        switch (infosourceNature) {
            case HAZARD_REPORT:
                formattedObjectFields.append(object.get(OBJECT_NAME.getKeyName()));
                break;
            case LIGHTHOUSE_AND_BEACON:
                final String heritageName = (String) object.get(LighthouseAndBeaconMessageDtoProperty.HERITAGE_NAME.getTranslateName());
                final String baptismName = (String) object.get(LighthouseAndBeaconMessageDtoProperty.BAPTISM_NAME.getTranslateName());
                final String marque = (String) object.get(LighthouseAndBeaconMessageDtoProperty.MARK.getTranslateName());
                final String identificationNumber = (String) object.get(LighthouseAndBeaconMessageDtoProperty.IDENTIFICATION_NUMBER.getTranslateName());
                final String listOfLightNumber = (String) object.get(LighthouseAndBeaconMessageDtoProperty.LIST_OF_LIGHTS.getTranslateName());
                final String comment = (String) infosourceObject.get(LighthouseAndBeaconMessageDtoProperty.COMMENT.getTranslateName());

                Integer indicationCode = (Integer) infosourceObject.get(LighthouseAndBeaconMessageDtoProperty.INDICATION_CODE.getTranslateName());

                if (isNotBlank(heritageName)) {
                    formattedObjectFields.append(heritageName);
                }
                if (isNotBlank(baptismName)) {
                    formattedObjectFields.append(" - " + baptismName);
                }
                if (isNotBlank(marque)) {
                    formattedObjectFields.append(" - " + orderMarkField(translationsRegistry.getLighthouseAndBeaconMessageByEnumNameAndLang(
                        LighthouseAndBeaconMessageDtoProperty.MARK.toString(), language), marque, language));
                }
                if (isNotBlank(identificationNumber)) {
                    formattedObjectFields.append(" - " + translationsRegistry.getLighthouseAndBeaconMessageByEnumNameAndLang(
                        LighthouseAndBeaconMessageDtoProperty.IDENTIFICATION_NUMBER.toString(), language) + " : " + identificationNumber);
                }
                formattedObjectFields.append("\n");

                if (isNotBlank(listOfLightNumber)) {
                    formattedObjectFields.append(translationsRegistry.getLighthouseAndBeaconMessageByEnumNameAndLang(
                        LighthouseAndBeaconMessageDtoProperty.LIST_OF_LIGHTS.toString(), language) + " : " + listOfLightNumber);
                }
                formattedObjectFields.append("\n");

                if (isNotBlank(comment)) {
                    formattedObjectFields.append(translationsRegistry.getLighthouseAndBeaconMessageByEnumNameAndLang(
                        LighthouseAndBeaconMessageDtoProperty.COMMENT.toString(), language) + " : " + comment + "\n");
                }
                if (indicationCode != null && indicationCode == 1) {
                    formattedObjectFields.append(getTranslatedText(language));
                }

                if (type != null) {
                    formattedObjectFields.append(translationsRegistry.getWarningHazardTypeDetailsByEnumNameAndLang(type.toString(), language));
                }
                break;

            case MAP_ANOMALY:
                final String mapNumber = (String) object.get(MAP_NUMBER.getKeyName());
                if (StringUtils.isBlank(mapNumber)) {
                    return "";
                }
                formattedObjectFields.append(mapNumber);
                final String editionNumber = (String) object.get(EDITION_NUMBER.getKeyName());
                final String lastPatchNumber = (String) object.get(LAST_PATCH_NUMBER.getKeyName());
                if (isNotBlank(editionNumber) && isNotBlank(lastPatchNumber)) {
                    formattedObjectFields.append(" (" + editionNumber + " - " + lastPatchNumber + ")");
                }
                break;
            case PUBLICATION_ANOMALY:
                final String issueReference = (String) object.get(ISSUE_REFERENCE.getKeyName());
                if (StringUtils.isBlank(issueReference)) {
                    return "";
                }
                formattedObjectFields.append(issueReference);
                final String upToDateAt = (String) object.get(UP_TO_DATE_AT.getKeyName());
                final String chapterOrPage = (String) object.get(CHAPTER_OR_PAGE.getKeyName());
                if (isNotBlank(upToDateAt) && isNotBlank(chapterOrPage)) {
                    formattedObjectFields.append(" (" + upToDateAt + " - " + chapterOrPage + ")");
                }
                break;
            case FUTURE_INFORMATION:
                formattedObjectFields.append(object.get(OBJECT_FUTURE_INFO.getKeyName()));
                break;
            default:
                break;
        }

        if (isNotBlank(formattedObjectFields)) {
            formattedObjectFields.append(DESCRIPTION_LINE_SEPARATOR);
        }

        return formattedObjectFields.toString();
    }

    private WarningHazardTypeDetails getWarningHazardTypeDetails(Map<String, Object> infosourceObject) {
        final Integer incidentCode = (Integer) ofNullable(infosourceObject.get(LighthouseAndBeaconMessageDtoProperty.INCIDENT_CODE.getTranslateName())).orElse(0);
        final Integer cmsCode = (Integer) ofNullable(infosourceObject.get(LighthouseAndBeaconMessageDtoProperty.CMS_CODE.getTranslateName())).orElse(0);
        final Integer natureCode = (Integer) ofNullable(infosourceObject.get(LighthouseAndBeaconMessageDtoProperty.NATURE_CODE.getTranslateName())).orElse(0);
        return WarningHazardTypeDetails.fromCodes(natureCode, cmsCode, incidentCode);
    }

    private String orderMarkField(String markField, String markValue, String lang) {
        if (lang.equals("fr")) {
            return markField + " " + markValue;
        }
        return markValue + " " + markField;
    }

    private String getTranslatedText(String lang) {
        if (lang.equals("fr")) {
            return "Information signalée \n";
        }
        return "Reported information \n";
    }

    public List<Infosource> findAllByIds(List<UUID> infosourceIds) {
        return repository.findAllById(infosourceIds);
    }

    public Set<InfosourceReferenceDto> getAvailableInfosourcesToLinkFromSerie(UUID serieId, String username) {
        final ServiceUser serviceUser = getServiceUser(username);

        final Serie serie = serieService.findById(serieId);
        final MaritimeService serieMaritimeService = serie.getMaritimeService();

        final boolean serviceUserHasNoRequiredRoleOnSerieService = serviceUser.getRoles().stream()
            .filter(serviceUserRole -> serieMaritimeService.getId().equals(serviceUserRole.getServiceId()))
            .noneMatch(serviceUserRole -> asList(SERVICE_OPERATOR, SERVICE_ADMIN).contains(serviceUserRole.getServiceRole()));

        if (serviceUserHasNoRequiredRoleOnSerieService) {
            throw new ForbiddenException();
        }

        return findByIssuingServiceOrRecipient(serieMaritimeService);
    }

    public Set<InfosourceReferenceDto> findByIssuingServiceOrRecipient(MaritimeService service) {
        return repository.findByIssuingServiceOrRecipient(service).stream()
            .map(InfosourceReferenceDto::fromInfosource)
            .collect(toSet());
    }

    public List<MaritimeEntityConcernedDto> getAvailableEntitiesForCreation(UUID serviceId, List<Geometry> geometries, String username) {
        ServiceUser serviceUser = getServiceUser(username);
        final boolean hasNoRequiredRoleOnService = checkIfUserHasNoRequiredRoleOnService(serviceUser, serviceId, asList(SERVICE_OPERATOR, SERVICE_ADMIN));

        if (hasNoRequiredRoleOnService) {
            throw new ForbiddenException();
        }

        return findAllConcernedMaritimeEntities(serviceId, geometries);
    }

    public List<MaritimeEntityConcernedDto> findAllConcernedMaritimeEntities(UUID serviceId, List<Geometry> geometries) {
        final MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("serviceId", serviceId);
        if (!isEmpty(geometries)) {
            final AtomicInteger geomId = new AtomicInteger(1);
            geometries.stream().forEach(
                geometry -> parameters.addValue("geom" + geomId.getAndIncrement(), "SRID=3857;" + geometry, Types.OTHER)
            );
        }

        final StringBuilder geometriesConditions = new StringBuilder();
        if (!isEmpty(geometries)) {
            geometriesConditions.append("union ");
            geometriesConditions.append("select me.id, me.name ");
            geometriesConditions.append("from maritime_entity me ");
            geometriesConditions.append("where me.type = 'SERVICE_AREA' ");
            geometriesConditions.append("and (");
            final AtomicInteger geomId = new AtomicInteger(1);
            geometriesConditions.append(
                geometries.stream().map(geometry -> "st_intersects(me.geom, :geom" + geomId.getAndIncrement() + ") = true").collect(joining(" OR "))
            );
            geometriesConditions.append(")");
        }

        return this.namedParameterJdbcTemplate.query(
            "select me.id, me.name " +
                "from maritime_entity me " +
                "where me.maritime_service_id = :serviceId " +
                "and me.type = 'SERVICE_AREA' " +
                "union " +
                "select meo.id, meo.name " +
                "from maritime_entity meo " +
                "inner join maritime_entity_additional_service meas on meas.maritime_service_id = :serviceId and meas.maritime_entity_id = meo.id " +
                "union " +
                "select me2.id , me2.name " +
                "from maritime_entity me " +
                "left join maritime_entity me2 on me2.id not in ( " +
                "select me.id " +
                "from maritime_entity me " +
                "where me.maritime_service_id = :serviceId " +
                "and me.type = 'SERVICE_AREA' " +
                ") " +
                "where me.maritime_service_id = :serviceId " +
                "and me.type = 'SERVICE_AREA' " +
                "and me2.type = 'SERVICE_AREA' " +
                "and st_intersects(me.geom, me2.geom) = true " +
                geometriesConditions,
            parameters,
            new BeanPropertyRowMapper<>(MaritimeEntityConcernedDto.class)
        );
    }

    public List<String> getAvailableTagsFromServiceId(UUID issuingServiceId) {
        return repository.findTagsByIssuingService(issuingServiceId);
    }

    @Transactional
    public UUID clone(UUID id, String username) {
        final ServiceUser serviceUser = getServiceUser(username);
        final Infosource infosource = repository.findById(id).orElseThrow(NotFoundException::new);

        if (List.of(GEOREGULATION, LIGHTHOUSE_AND_BEACON, BATHY, SURVEY_TRANSMISSION).contains(infosource.getNature())) {
            throw new BadRequestException("Cannot copy infosource with id " + id + " because of its nature " + infosource.getNature());
        }

        boolean hasNoRoleOnService = false;

        // check user has any right on IS issuing service
        if (infosource.getIssuingService() != null) {
            hasNoRoleOnService = hasNoRoleOnService(serviceUser, infosource.getIssuingService().getId());
        }

        // check user has any right on IS recipient service
        final boolean hasNoRoleOnRecipientService = hasNoRoleOnInfosourceRecipientService(serviceUser, infosource);

        if (hasNoRoleOnService && hasNoRoleOnRecipientService) {
            throw new ForbiddenException();
        }

        final Instant now = now();

        final Infosource clonedInfosource = Infosource.builder()
            .issuingService(serviceUser.getService())
            .issuingServiceUser(serviceUser)
            .entities(newHashSet(infosource.getEntities()))
            .nature(infosource.getNature())
            .status(DRAFT)
            .timezone(infosource.getTimezone())
            .object(Optional.ofNullable(infosource.getObject()).map(ImmutableMap::copyOf).orElse(null))
            .creationDate(now)
            .lastUpdate(now)
            .infosourceParts(cloneInfosourceParts(infosource))
            .parentInfosource(infosource)
            .build();

        final Infosource savedInfosource = repository.save(clonedInfosource);

        // schedule IS draft deletion
        schedulerJobService.scheduleAutoDeleteDraftInfosourceJob(savedInfosource);

        folderService.addInfosourceToInfosourceFolders(id, savedInfosource);

        return savedInfosource.getId();
    }

    private ImmutableSortedSet<InfosourcePart> cloneInfosourceParts(Infosource infosource) {
        return infosource.getInfosourceParts().stream()
            .map(infosourcePart -> InfosourcePart.builder()
                .position(infosourcePart.getPosition())
                .description(infosourcePart.getDescription())
                .internationalDescription(infosourcePart.getInternationalDescription())
                .object(Optional.ofNullable(infosourcePart.getObject()).map(ImmutableMap::copyOf).orElse(null))
                .localisation(newHashSet(infosourcePart.getLocalisation()))
                .build())
            .collect(toImmutableSortedSet(comparing(InfosourcePart::getPosition)));
    }

    private static boolean checkIfUserHasNoRequiredRoleOnService(ServiceUser serviceUser, UUID serviceId, List<ServiceRole> serviceRoles) {
        return serviceUser.getRoles().stream()
            .filter(serviceUserRole -> serviceRoles.contains(serviceUserRole.getServiceRole()))
            .map(ServiceUserRole::getServiceId)
            .noneMatch(uuid -> uuid.equals(serviceId));
    }

    private static boolean checkIfUserHasRightOnIssuingServiceOrRecipientsServicesOfTag(Tag tag, UUID issuingServiceId, List<UUID> recipientsId,
        ServiceUser serviceUser) {
        return (tag.getMaritimeService().getId() == issuingServiceId && serviceUser.getService().getId() == issuingServiceId)
            || (tag.getMaritimeService().getId() == issuingServiceId
            && !checkIfUserHasNoRequiredRoleOnService(serviceUser, issuingServiceId, asList(SERVICE_OPERATOR, SERVICE_ADMIN)))
            || (recipientsId.contains(tag.getMaritimeService().getId())
            && !checkIfUserHasNoRequiredRoleOnService(serviceUser, tag.getMaritimeService().getId(), asList(SERVICE_OPERATOR, SERVICE_ADMIN)));
    }

    @Transactional
    public void linkTagsToInfosource(UUID infosourceId, List<UUID> tagsIdsToLink, String username) {
        ServiceUser serviceUser = getServiceUser(username);

        Infosource infosource = repository.findById(infosourceId).orElseThrow(ForbiddenException::new);
        Set<Tag> tagsToLink = tagService.findAllById(tagsIdsToLink);

        UUID issuingServiceId = Optional.ofNullable(infosource.getIssuingService()).map(MaritimeService::getId).orElse(null);

        List<UUID> recipientsId = infosource.getRecipients().stream()
            .map(infosourceRecipient -> infosourceRecipient.getService().getId())
            .collect(toList());

        tagsToLink.forEach(tag -> {
                boolean hasNoRequiredRoleOnService = !checkIfUserHasRightOnIssuingServiceOrRecipientsServicesOfTag(tag, issuingServiceId, recipientsId,
                    serviceUser);
                if (hasNoRequiredRoleOnService) {
                    throw new ForbiddenException();
                }
            }
        );

        if (infosource.getStatus() == DRAFT) {
            infosource.setTags(tagsToLink.stream().filter(tag -> tag.getMaritimeService().getId() == infosource.getIssuingService().getId()).collect(Collectors.toSet()));
            repository.save(infosource);
            return;
        }

        Set<Tag> infosourceTags = infosource.getTags();

        Set<Tag> tagsOnWhichUserHaveNoRight = newHashSet();

        Set<Tag> tagsOnWhichUserHaveRight = newHashSet();

        infosourceTags.forEach(tag -> {
            boolean userHasRightOnIssuingServiceOrRecipientsServicesOfTag =
                checkIfUserHasRightOnIssuingServiceOrRecipientsServicesOfTag(tag, issuingServiceId, recipientsId, serviceUser);
            if (userHasRightOnIssuingServiceOrRecipientsServicesOfTag) {
                tagsOnWhichUserHaveRight.add(tag);
            } else {
                tagsOnWhichUserHaveNoRight.add(tag);
            }
        });

        Set<Tag> tagsToSet = newHashSet();
        Set<Tag> newTagsToLink = tagsToLink.stream().filter(tag -> !tagsOnWhichUserHaveRight.contains(tag)).collect(toSet());

        tagsToSet.addAll(tagsOnWhichUserHaveRight.stream().filter(tagsToLink::contains).collect(toSet()));
        tagsToSet.addAll(newTagsToLink);
        tagsToSet.addAll(tagsOnWhichUserHaveNoRight);

        infosource.setTags(tagsToSet);

        repository.save(infosource);
    }

    public List<UUID> getTagsIdsFromInfosourceId(UUID infosourceId, String username) {
        Infosource infosource = repository.findById(infosourceId).orElseThrow(ForbiddenException::new);

        List<UUID> recipientsId = infosource.getRecipients().stream()
            .map(infosourceRecipient -> infosourceRecipient.getService().getId())
            .collect(toList());

        UUID issuingServiceId = Optional.ofNullable(infosource.getIssuingService()).map(MaritimeService::getId).orElse(null);

        ServiceUser serviceUser = getServiceUser(username);

        Set<Tag> tagsOnWhichUserHaveRight;

        if (infosource.getStatus() == DRAFT) {
            tagsOnWhichUserHaveRight = infosource.getTags().stream()
                .filter(tag ->
                    (tag.getMaritimeService().getId() == issuingServiceId) && !checkIfUserHasNoRequiredRoleOnService(serviceUser, issuingServiceId,
                        asList(SERVICE_OPERATOR, SERVICE_ADMIN)))
                .collect(Collectors.toSet());
        } else {
            tagsOnWhichUserHaveRight = infosource.getTags().stream()
                .filter(tag ->
                    checkIfUserHasRightOnIssuingServiceOrRecipientsServicesOfTag(tag, issuingServiceId, recipientsId, serviceUser))
                .collect(Collectors.toSet());
        }

        return tagsOnWhichUserHaveRight.stream().map(Tag::getId).collect(toList());
    }

    @Transactional
    public void createAndSendGeoregulation(Georegulation georegulation, Set<UUID> recipientIds, ServiceUser serviceUser) {
        boolean infosourceAlreadyExists = repository.existsByGeoregulationId(georegulation.getId());

        if (infosourceAlreadyExists) {
            throw new BadRequestException("Infosource from georegulation with id " + georegulation.getId() + " already exists");
        }

        AtomicInteger infosourcePartPosition = new AtomicInteger(1);
        Instant now = now();

        GeoregulationNumber georegulationNumber = georegulation.getNumber();
        String number = GeoregulationUtil.getNumber(
            georegulationNumber.getYear(),
            georegulationNumber.getText(),
            newHashSet(georegulationNumber.getAuthorityCodes()),
            georegulationNumber.getDate()
        );

        Map<String, Object> object = new HashMap<>();
        object.put(InfosourcePartObjectKeys.GEOREGULATION_NUMBER.getKeyName(), number);
        if (isNotEmpty(georegulation.getAdditionalNumbers())) {
            object.put(InfosourcePartObjectKeys.GEOREGULATION_ADDITIONAL_NUMBERS.getKeyName(), georegulation.getAdditionalNumbers().stream()
                .map(additionalNumber -> GeoregulationUtil.getNumber(
                    additionalNumber.getYear(),
                    additionalNumber.getText(),
                    newHashSet(additionalNumber.getAuthorityCodes()),
                    additionalNumber.getDate()))
                .collect(toList()));
        }
        if (isNotEmpty(georegulation.getCalendarPeriods())) {
            object.put(GEOREGULATION_CALENDAR_PERIODS.getKeyName(), georegulation.getCalendarPeriods());
        }
        if (isNotEmpty(georegulation.getSeasonalPeriods())) {
            object.put(GEOREGULATION_SEASONAL_PERIODS.getKeyName(), georegulation.getSeasonalPeriods());
        }
        if (georegulation.getType() != null) {
            object.put(InfosourcePartObjectKeys.GEOREGULATION_TYPE.getKeyName(), georegulation.getType());
        }
        if (isNotEmpty(georegulation.getActivities())) {
            object.put(InfosourcePartObjectKeys.GEOREGULATION_ACTIVITIES.getKeyName(), georegulation.getActivities());
        }
        if (isNotBlank(georegulation.getTitle())) {
            object.put(InfosourcePartObjectKeys.GEOREGULATION_TITLE.getKeyName(), georegulation.getTitle());
        }
        if (isNotBlank(georegulation.getSummary())) {
            object.put(InfosourcePartObjectKeys.GEOREGULATION_SUMMARY.getKeyName(), georegulation.getSummary());
        }
        if (isNotEmpty(georegulation.getEntities())) {
            object.put(InfosourcePartObjectKeys.GEOREGULATION_MARITIME_ENTITY.getKeyName(),
                georegulation.getEntities().stream().map(MaritimeEntity::getName).collect(toList()));
        }

        Infosource infosource = Infosource.builder()
            .number(generateInfosourceNumber(serviceUser))
            .infosourceParts(new TreeSet<>(
                georegulation.getParts().stream()
                    .map(part -> {
                            final InfosourcePart infosourcePart = InfosourcePart.builder()
                                .position(infosourcePartPosition.getAndIncrement())
                                .localisation(new HashSet<>(part.getFeatures()))
                                .description(number)
                                .object(new HashMap<>(object))
                                .build();
                            infosourcePart.getObject().put(GEOREGULATION_PART_DETAILS.getKeyName(), buildGeoregulationPartInfos(part));
                            return infosourcePart;
                        }
                    )
                    .collect(toList())))
            .nature(GEOREGULATION)
            .status(SENT)
            .issuingService(serviceUser.getService())
            .issuingServiceUser(serviceUser)
            .timezone(georegulation.getTimezone())
            .entities(new HashSet<>(georegulation.getEntities()))
            .creationDate(now)
            .lastUpdate(now)
            .validationTime(now)
            .georegulation(georegulation)
            .build();

        Infosource savedInfosource = repository.save(infosource);
        List<MaritimeService> recipients = maritimeServiceRepository.findByIdIn(new ArrayList<>(recipientIds));
        recipientService.send(recipients, savedInfosource, serviceUser.getId(), PRO, true);
        folderService.addInfosourceToGeoregulationFolders(georegulation.getId(), savedInfosource);
    }

    private static Map<String, Object> buildGeoregulationPartInfos(GeoregulationPart georegulationPart) {
        final Map<String, Object> mapGeoregulationPartInfos = new HashMap<>();

        mapGeoregulationPartInfos.put(GEOREGULATION_PART_NUMBER.getKeyName(), georegulationPart.getPosition());

        if (isNotEmpty(georegulationPart.getTypes())) {
            mapGeoregulationPartInfos.put(GEOREGULATION_PART_TYPES.getKeyName(), georegulationPart.getTypes());
        }

        if (isNotEmpty(georegulationPart.getCalendarPeriods())) {
            mapGeoregulationPartInfos.put(GEOREGULATION_CALENDAR_PERIODS.getKeyName(), georegulationPart.getCalendarPeriods());
        }

        if (isNotEmpty(georegulationPart.getSeasonalPeriods())) {
            mapGeoregulationPartInfos.put(GEOREGULATION_SEASONAL_PERIODS.getKeyName(), georegulationPart.getSeasonalPeriods());
        }

        if (isNotBlank(georegulationPart.getInformation())) {
            mapGeoregulationPartInfos.put(GEOREGULATION_PART_INFORMATION.getKeyName(), georegulationPart.getInformation());
        }

        if (isNotBlank(georegulationPart.getInternationalInformation())) {
            mapGeoregulationPartInfos.put(GEOREGULATION_PART_INTERNATIONAL_INFORMATION.getKeyName(), georegulationPart.getInternationalInformation());
        }

        return mapGeoregulationPartInfos;
    }

    private UUID createFromGeoregulation(Serie serie, ServiceUser serviceUser, Infosource infosource, boolean copyData) {
        Georegulation georegulation = infosource.getGeoregulation();
        Warning warning = Warning.builder()
            .status(WarningStatus.DRAFT)
            .serie(serie)
            .nameOfSeries(serie.getName())
            .productionAgency(serie.getMaritimeService().getProductionAgency())
            .warningHazardTypeGeneral(ROUTING_INFORMATION)
            .infosources(newHashSet(infosource))
            .georegulations(newHashSet(WarningGeoregulation.builder().georegulation(georegulation).build()))
            .lastUpdate(now())
            .lastUpdateUser(serviceUser)
            .build();

        Warning finalWarning = warning;
        warning.getGeoregulations().forEach(warningGeoregulation -> warningGeoregulation.setWarning(finalWarning));

        if (copyData) {
            String number = GeoregulationUtil.getFullNumber(
                georegulation.getNumber().getYear(),
                georegulation.getNumber().getText(),
                georegulation.getNumber().getAuthorities(),
                georegulation.getNumber().getDate(),
                false
            );

            String internationalNumber = GeoregulationUtil.getFullNumber(
                georegulation.getNumber().getYear(),
                georegulation.getNumber().getText(),
                georegulation.getNumber().getAuthorities(),
                georegulation.getNumber().getDate(),
                true
            );

            AtomicInteger atomicInteger = new AtomicInteger(1);
            TreeSet<FeaturePart> featureParts = georegulation.getParts().stream()
                .map(part -> {
                    String typesTranslations = StringUtils.capitalize(part.getTypes().stream()
                        .map(type -> translationsRegistry.getGeoregulationPartTypeTranslation(type, GeoregulationPartTypeEntity::getTranslation).toLowerCase())
                        .collect(joining(", ")));
                    String typesInternationalTranslations = StringUtils.capitalize(part.getTypes().stream()
                        .map(type -> translationsRegistry.getGeoregulationPartTypeTranslation(type,
                            GeoregulationPartTypeEntity::getInternationalTranslation).toLowerCase())
                        .collect(joining(", ")));

                    return FeaturePart.builder()

                        .position(atomicInteger.getAndIncrement())
                        .geometries(newHashSet(part.getFeatures()))
                        .information(newHashSet(asList(
                            LocalizedText.builder()
                                .language(FRANCE.getLanguage())
                                .text(Stream.of(typesTranslations, number, part.getInformation())
                                    .filter(StringUtils::isNotBlank)
                                    .collect(joining("\n")))
                                .build(),
                            LocalizedText.builder()
                                .language(ENGLISH.getLanguage())
                                .text(Stream.of(typesInternationalTranslations, internationalNumber, part.getInternationalInformation())
                                    .filter(StringUtils::isNotBlank)
                                    .collect(joining("\n")))
                                .build()
                        )))
                        .fixedDateRanges(newTreeSet(
                            part.getCalendarPeriods().stream()
                                .map(period -> {
                                    FixedDateRange mappedPeriod = new FixedDateRange();
                                    if (period.getDateStart() != null) {
                                        ZonedDateTime zonedDateStart = period.getDateStart()
                                            .atTime(0, 0, 0)
                                            .atZone(ZoneId.of(convertToUtcHoursAndMinutes(georegulation.getTimezone())))
                                            .withZoneSameInstant(UTC);
                                        mappedPeriod.setDateStart(zonedDateStart.toLocalDate());
                                        mappedPeriod.setTimeOfDayStart(zonedDateStart.toLocalTime());
                                    }

                                    if (period.getDateEnd() != null) {
                                        ZonedDateTime zonedDateEnd = period.getDateEnd()
                                            .atTime(23, 59, 59)
                                            .atZone(ZoneId.of(convertToUtcHoursAndMinutes(georegulation.getTimezone())))
                                            .withZoneSameInstant(UTC);
                                        mappedPeriod.setDateEnd(zonedDateEnd.toLocalDate());
                                        mappedPeriod.setTimeOfDayEnd(zonedDateEnd.toLocalTime());
                                    }
                                    mappedPeriod.setTimezone(georegulation.getTimezone());

                                    return mappedPeriod;
                                })
                                .collect(toList())
                        ))
                        .build();
                }).collect(toCollection(TreeSet::new));

            warning = warning.toBuilder()
                .featureParts(featureParts)
                .build();
        }

        Warning savedWarning = warningRepository.save(warning);

        folderService.addWarningToInfosourceFolders(infosource.getId(), savedWarning);

        return savedWarning.getId();
    }

    public static void checkIfLocalisationIsEmpty(Set<FeatureDto> localisation) {
        if (isEmpty(localisation)) {
            throw new BadRequestException("validation.infosource-part.localisation.not-empty");
        }
    }

    @Transactional
    public UUID createInfosourceBathy(Survey survey, ServiceUser serviceUser) {
        final Infosource infosource = createInfosourceBathyFromSurvey(survey, serviceUser)
            .issuingServiceUser(serviceUser)
            .issuingService(serviceUser.getService())
            .build();

        final Infosource savedInfosource = repository.save(infosource);

        // schedule IS draft deletion
        schedulerJobService.scheduleAutoDeleteDraftInfosourceJob(savedInfosource);

        return savedInfosource.getId();
    }

    @Transactional
    public Infosource createInfosourceDeposit(Survey survey, ServiceUser serviceUser) {
        // DPC_RG_13160
        final Infosource infosource = createInfosourceDepositFromSurvey(survey, serviceUser)
            .issuingServiceUser(serviceUser)
            .issuingService(serviceUser.getService())
            .build();
        return repository.save(infosource);
    }

    @Transactional
    @SuppressWarnings("java:S103")
    public UUID updateBathyReport(UUID id, InfosourceBathyDangerReportCreateWithServiceUserDto dto, BindingResult bindingResult, String username) throws
        ServiceException {
        checkIfLocalisationIsEmpty(dto.getLocalisation());
        checkBathyGeometries(dto);
        return updateReport(id, dto, bindingResult, username);
    }

    private UUID createFromInfosourceBathy(Serie serie, ServiceUser serviceUser, Infosource infosource, boolean copyData) {
        Survey survey = infosource.getSurvey();
        Warning warning = Warning.builder()
            .status(WarningStatus.DRAFT)
            .serie(serie)
            .nameOfSeries(serie.getName())
            .productionAgency(serie.getMaritimeService().getProductionAgency())
            .warningHazardTypeGeneral(NEWLY_DISCOVERED_DANGERS)
            .infosources(newHashSet(infosource))
            .survey(survey)
            .lastUpdate(now())
            .isBathy(true)
            .lastUpdateUser(serviceUser)
            .build();

        if (copyData) {
            FixedDateRange mappedPeriod = new FixedDateRange();
            if (infosource.getStartDate() != null) {
                ZonedDateTime zonedDateStart = LocalDate.ofInstant(infosource.getStartDate(), UTC)
                    .atTime(0, 0, 0)
                    .atZone(ZoneId.of(String.valueOf(UTC)))
                    .withZoneSameInstant(UTC);
                mappedPeriod.setDateStart(zonedDateStart.toLocalDate());
                mappedPeriod.setTimeOfDayStart(zonedDateStart.toLocalTime());
            }

            if (infosource.getEndDate() != null) {
                ZonedDateTime zonedDateEnd = LocalDate.ofInstant(infosource.getEndDate(), UTC)
                    .atTime(0, 0, 0)
                    .atZone(ZoneId.of(String.valueOf(UTC)))
                    .withZoneSameInstant(UTC);
                mappedPeriod.setDateEnd(zonedDateEnd.toLocalDate());
                mappedPeriod.setTimeOfDayEnd(zonedDateEnd.toLocalTime());
            }

            AtomicInteger atomicInteger = new AtomicInteger(1);

            String ownerOrganization = survey.getOwnerOrganization();
            String textForInputInformation = (StringUtils.isNotBlank(ownerOrganization) ? ownerOrganization.concat(", ") : "")
                .concat(survey.getNameAndLocation() + ", " + survey.getIdentifier());

            SortedSet<FeaturePart> featureParts = infosource.getInfosourceParts().stream().findFirst().get().getLocalisation()
                .stream().map(featurePart -> FeaturePart.builder()
                    .position(atomicInteger.getAndIncrement())
                    .geometries(newHashSet(Feature.builder().geometry(featurePart.getGeometry()).properties(featurePart.getProperties()).build()))
                    .information(newHashSet(
                        LocalizedText.builder()
                            .language(FRANCE.getLanguage())
                            .text(textForInputInformation)
                            .build(),
                        LocalizedText.builder()
                            .language(ENGLISH.getLanguage())
                            .text(textForInputInformation)
                            .build()))
                    .fixedDateRanges(newTreeSet(
                        List.of(mappedPeriod)
                    ))
                    .build()
                ).collect(toCollection(TreeSet::new));

            warning = warning.toBuilder()
                .featureParts(featureParts)
                .build();
        }

        Warning savedWarning = warningRepository.save(warning);

        folderService.addWarningToInfosourceFolders(infosource.getId(), savedWarning);

        return savedWarning.getId();
    }
}
