package fr.shom.ping.service;

import fr.shom.ping.exception.BadRequestException;
import fr.shom.ping.exception.ForbiddenException;
import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.Attachment;
import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.InfosourcePart;
import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.PublicUser;
import fr.shom.ping.model.Survey;
import fr.shom.ping.model.User;
import fr.shom.ping.model.dto.InfosourceActivityForecastCreateWithPublicUserDto;
import fr.shom.ping.model.dto.InfosourceActivityForecastDetailWithPublicUserDto;
import fr.shom.ping.model.dto.InfosourceBathyDangerReportCreateWithPublicUserDto;
import fr.shom.ping.model.dto.InfosourceBathyDangerReportDetailWithPublicUserDto;
import fr.shom.ping.model.dto.InfosourceBathyDetailDto;
import fr.shom.ping.model.dto.InfosourceDetailDto;
import fr.shom.ping.model.dto.InfosourceFutureUpdatesCreateWithPublicUserDto;
import fr.shom.ping.model.dto.InfosourceFutureUpdatesDetailWithPublicUserDto;
import fr.shom.ping.model.dto.InfosourceHazardReportCreateWithPublicUserDto;
import fr.shom.ping.model.dto.InfosourceHazardReportDetailWithPublicUserDto;
import fr.shom.ping.model.dto.InfosourceMapAnomalyCreateWithPublicUserDto;
import fr.shom.ping.model.dto.InfosourceMapAnomalyDetailWithPublicUserDto;
import fr.shom.ping.model.dto.InfosourceOtherReportCreateWithPublicUserDto;
import fr.shom.ping.model.dto.InfosourceOtherReportDetailWithPublicUserDto;
import fr.shom.ping.model.dto.InfosourcePublicationAnomalyCreateWithPublicUserDto;
import fr.shom.ping.model.dto.InfosourcePublicationAnomalyReportDetailWithPublicUserDto;
import fr.shom.ping.model.dto.InfosourceReportAbstractCreateDto;
import fr.shom.ping.model.dto.InfosourceServiceStatusServiceUserDto;
import fr.shom.ping.model.dto.InfosourceTableDto;
import fr.shom.ping.model.dto.InfosourceToDisplayOnCreationMapDto;
import fr.shom.ping.model.dto.MaritimeServiceInfosourceRecipientDto;
import fr.shom.ping.model.enums.InfosourceNature;
import fr.shom.ping.model.predicate.InfosourceSearchCriteria;
import fr.shom.ping.properties.PingProperties;
import fr.shom.ping.repository.InfosourceRepository;
import fr.shom.ping.repository.MaritimeServiceRepository;
import fr.shom.ping.repository.UserRepository;
import fr.shom.ping.scheduling.SchedulerJobService;
import fr.shom.ping.utils.InfosourceUtil;
import fr.shom.ping.utils.search.InfosourceTableUtil;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

import static com.google.common.collect.Sets.newHashSet;
import static fr.shom.ping.exception.ThrowingConsumerWrapper.throwingConsumerWrapper;
import static fr.shom.ping.model.enums.InfosourceStatus.DRAFT;
import static fr.shom.ping.model.enums.UserType.PUBLIC;
import static fr.shom.ping.service.InfosourceAdminService.checkIfLocalisationIsEmpty;
import static fr.shom.ping.service.SurveyExternalAnalysisService.getLastProcessingResult;
import static java.time.Instant.now;
import static java.util.stream.Collectors.toList;

@Service
public class InfosourceSecuredService extends AbstractInfosourceService {

    public static final String FEATURE_PART_EXCEPTION = "Information is not filled for at least one feature part";

    private final UserRepository userRepository;

    public InfosourceSecuredService(EntityManager entityManager, InfosourceRepository repository, AttachmentService attachmentService,
        InfosourceRoutingService routingService, UserRepository userRepository,
        MaritimeServiceRepository maritimeServiceRepository, InfosourceRecipientService recipientService, SchedulerJobService schedulerJobService,
        FolderService folderService, PingProperties pingProperties) {
        super(entityManager, repository, attachmentService, routingService, maritimeServiceRepository, recipientService, schedulerJobService, folderService, pingProperties);
        this.userRepository = userRepository;
    }

    public List<InfosourceToDisplayOnCreationMapDto> findAllToDisplayOnCreationMap(UUID infosourceId, String username) {
        final PublicUser publicUser = getPublicUser(username);
        if (infosourceId == null) {
            return InfosourceUtil.groupInfosourceToDisplayOnCreationMapTupleByInfosourceId(
                repository.findAllToDisplayOnCreationMapByPublicUserId(publicUser.getId())
            );
        }

        return InfosourceUtil.groupInfosourceToDisplayOnCreationMapTupleByInfosourceId(
            repository.findAllToDisplayOnCreationMapByPublicUserIdAndExcludedInfosource(infosourceId, publicUser.getId())
        );
    }

    @Transactional
    public UUID createHazardReport(InfosourceHazardReportCreateWithPublicUserDto dto, BindingResult bindingResult, String username) throws ServiceException {
        PublicUser publicUser = getPublicUser(bindingResult, username);

        checkIfLocalisationIsEmpty(dto.getLocalisation());
        List<Attachment> attachments = attachmentService.findAllByIdIn(dto.getAttachmentIds());
        Infosource infosource = dto.toInfosource(publicUser, attachments);

        final Infosource savedInfosource = repository.save(infosource);

        // schedule IS draft deletion
        schedulerJobService.scheduleAutoDeleteDraftInfosourceJob(savedInfosource);

        return savedInfosource.getId();
    }

    @Transactional
    public UUID createOtherReport(InfosourceOtherReportCreateWithPublicUserDto dto, BindingResult bindingResult, String username) throws ServiceException {
        PublicUser publicUser = getPublicUser(bindingResult, username);

        checkIfLocalisationIsEmpty(dto.getLocalisation());
        List<Attachment> attachments = attachmentService.findAllByIdIn(dto.getAttachmentIds());
        Infosource infosource = dto.toInfosource(publicUser, attachments);

        final Infosource savedInfosource = repository.save(infosource);

        // schedule IS draft deletion
        schedulerJobService.scheduleAutoDeleteDraftInfosourceJob(savedInfosource);

        return savedInfosource.getId();
    }

    @Transactional
    public UUID createMapAnomaly(InfosourceMapAnomalyCreateWithPublicUserDto dto, BindingResult bindingResult, String username) throws ServiceException {
        PublicUser publicUser = getPublicUser(bindingResult, username);

        checkIfLocalisationIsEmpty(dto.getLocalisation());
        List<Attachment> attachments = attachmentService.findAllByIdIn(dto.getAttachmentIds());
        Infosource infosource = dto.toInfosource(publicUser, attachments);

        final Infosource savedInfosource = repository.save(infosource);

        // schedule IS draft deletion
        schedulerJobService.scheduleAutoDeleteDraftInfosourceJob(savedInfosource);

        return savedInfosource.getId();
    }

    @Transactional
    public UUID createFutureUpdates(InfosourceFutureUpdatesCreateWithPublicUserDto dto, BindingResult bindingResult, String username) throws ServiceException {
        PublicUser publicUser = getPublicUser(bindingResult, username);

        checkIfLocalisationIsEmpty(dto.getLocalisation());
        List<Attachment> attachments = attachmentService.findAllByIdIn(dto.getAttachmentIds());
        Infosource infosource = dto.toInfosource(publicUser, attachments);

        final Infosource savedInfosource = repository.save(infosource);

        // schedule IS draft deletion
        schedulerJobService.scheduleAutoDeleteDraftInfosourceJob(savedInfosource);

        return savedInfosource.getId();
    }

    @Transactional
    @SuppressWarnings("java:S103")
    public UUID createPublicationAnomaly(InfosourcePublicationAnomalyCreateWithPublicUserDto dto, BindingResult bindingResult, String username) throws ServiceException {
        PublicUser publicUser = getPublicUser(bindingResult, username);

        checkIfLocalisationIsEmpty(dto.getLocalisation());
        List<Attachment> attachments = attachmentService.findAllByIdIn(dto.getAttachmentIds());
        Infosource infosource = dto.toInfosource(publicUser, attachments);

        final Infosource savedInfosource = repository.save(infosource);

        // schedule IS draft deletion
        schedulerJobService.scheduleAutoDeleteDraftInfosourceJob(savedInfosource);

        return savedInfosource.getId();
    }

    @Transactional
    @SuppressWarnings("java:S103")
    public UUID createActivityForecast(InfosourceActivityForecastCreateWithPublicUserDto dto, BindingResult bindingResult, String username) throws ServiceException {
        final PublicUser publicUser = getPublicUser(bindingResult, username);

        final List<Attachment> attachments = attachmentService.findAllByIdIn(dto.getAttachmentIds());
        final Infosource infosource = dto.toInfosource(publicUser, attachments);

        final Infosource savedInfosource = repository.save(infosource);

        // schedule IS draft deletion
        schedulerJobService.scheduleAutoDeleteDraftInfosourceJob(savedInfosource);

        return savedInfosource.getId();
    }

    @Transactional
    @SuppressWarnings("java:S103")
    public UUID updateHazardReport(UUID id, InfosourceHazardReportCreateWithPublicUserDto dto, BindingResult bindingResult, String username) throws ServiceException {
        PublicUser publicUser = getPublicUser(bindingResult, username);

        checkIfLocalisationIsEmpty(dto.getLocalisation());
        List<Attachment> attachments = attachmentService.findAllByIdIn(dto.getAttachmentIds());
        Infosource infosourceToUpdate = dto.toInfosource(publicUser, attachments);

        return updateInfosource(id, infosourceToUpdate, publicUser);
    }

    @Transactional
    @SuppressWarnings("java:S103")
    public UUID updateOtherReport(UUID id, InfosourceOtherReportCreateWithPublicUserDto dto, BindingResult bindingResult, String username) throws ServiceException {
        PublicUser publicUser = getPublicUser(bindingResult, username);

        checkIfLocalisationIsEmpty(dto.getLocalisation());
        List<Attachment> attachments = attachmentService.findAllByIdIn(dto.getAttachmentIds());
        Infosource infosourceToUpdate = dto.toInfosource(publicUser, attachments);

        return updateInfosource(id, infosourceToUpdate, publicUser);
    }

    @Transactional
    @SuppressWarnings("java:S103")
    public UUID updateMapAnomaly(UUID id, InfosourceMapAnomalyCreateWithPublicUserDto dto, BindingResult bindingResult, String username) throws ServiceException {
        PublicUser publicUser = getPublicUser(bindingResult, username);

        checkIfLocalisationIsEmpty(dto.getLocalisation());
        List<Attachment> attachments = attachmentService.findAllByIdIn(dto.getAttachmentIds());
        Infosource infosourceToUpdate = dto.toInfosource(publicUser, attachments);

        return updateInfosource(id, infosourceToUpdate, publicUser);
    }

    @Transactional
    @SuppressWarnings("java:S103")
    public UUID updateFutureUpdates(UUID id, InfosourceFutureUpdatesCreateWithPublicUserDto dto, BindingResult bindingResult, String username) throws ServiceException {
        PublicUser publicUser = getPublicUser(bindingResult, username);

        checkIfLocalisationIsEmpty(dto.getLocalisation());
        List<Attachment> attachments = attachmentService.findAllByIdIn(dto.getAttachmentIds());
        Infosource infosourceToUpdate = dto.toInfosource(publicUser, attachments);

        return updateInfosource(id, infosourceToUpdate, publicUser);
    }

    @Transactional
    @SuppressWarnings("java:S103")
    public UUID updatePublicationAnomaly(UUID id, InfosourcePublicationAnomalyCreateWithPublicUserDto dto, BindingResult bindingResult, String username) throws ServiceException {
        PublicUser publicUser = getPublicUser(bindingResult, username);

        checkIfLocalisationIsEmpty(dto.getLocalisation());
        List<Attachment> attachments = attachmentService.findAllByIdIn(dto.getAttachmentIds());
        Infosource infosourceToUpdate = dto.toInfosource(publicUser, attachments);

        return updateInfosource(id, infosourceToUpdate, publicUser);
    }

    @Transactional
    @SuppressWarnings("java:S103")
    public UUID updateActivityForecast(UUID id, InfosourceActivityForecastDetailWithPublicUserDto dto, BindingResult bindingResult, String username)
        throws ServiceException {
        final PublicUser publicUser = getPublicUser(bindingResult, username);

        final List<Attachment> attachments = attachmentService.findAllByIdIn(dto.getAttachmentIds());
        final Infosource infosourceToUpdate = dto.toInfosource(publicUser, attachments);

        final Infosource infosource = getInfosource(id, publicUser);
        infosource.setTimezone(infosourceToUpdate.getTimezone());

        // For each part, check if at least information is filled
        boolean atLeastInformationIsFilledForEachPart = dto.getParts().stream()
            .allMatch(part -> !part.getInformation().isEmpty());

        if (!atLeastInformationIsFilledForEachPart) {
            throw new BadRequestException(FEATURE_PART_EXCEPTION);
        }
        final SortedSet<InfosourcePart> featureParts = infosourceToUpdate.getInfosourceParts();
        featureParts.forEach(featurePart -> featurePart.setInfosource(infosource));
        infosource.getInfosourceParts().clear();
        infosource.getInfosourceParts().addAll(infosourceToUpdate.getInfosourceParts());

        final List<UUID> infosourceUpdatedAttachmentsIds = infosourceToUpdate.getAttachments().stream().map(Attachment::getId).collect(toList());
        deleteUnusedAttachments(infosource, infosourceUpdatedAttachmentsIds);
        final Set<Attachment> attachmentsUpdated = newHashSet(infosourceToUpdate.getAttachments());
        infosource.getAttachments().clear();
        infosource.getAttachments().addAll(attachmentsUpdated);

        infosource.setLastUpdate(now());

        final Infosource savedInfosource = repository.save(infosource);

        // remove scheduling of deletion && schedule new deletion with lastUpdate + 30 days
        schedulerJobService.scheduleAutoDeleteDraftInfosourceJob(savedInfosource);

        return savedInfosource.getId();
    }

    public InfosourceReportAbstractCreateDto findById(UUID id, String username) {
        PublicUser publicUser = userRepository.findOneByEmail(username);
        if (publicUser == null) {
            throw new ForbiddenException();
        }
        Infosource infosource = getInfosource(id, publicUser);

        switch (infosource.getNature()) {
            case HAZARD_REPORT:
                return new InfosourceHazardReportDetailWithPublicUserDto(infosource);
            case OTHER_REPORT:
                return new InfosourceOtherReportDetailWithPublicUserDto(infosource);
            case MAP_ANOMALY:
                return new InfosourceMapAnomalyDetailWithPublicUserDto(infosource);
            case PUBLICATION_ANOMALY:
                return new InfosourcePublicationAnomalyReportDetailWithPublicUserDto(infosource);
            case FUTURE_INFORMATION:
                return new InfosourceFutureUpdatesDetailWithPublicUserDto(infosource);
            case BATHY:
                return new InfosourceBathyDangerReportDetailWithPublicUserDto(infosource, getLastProcessingResult(infosource.getSurvey()).getDetections());
            default:
                throw new NotFoundException();
        }

    }

    public InfosourceActivityForecastDetailWithPublicUserDto findActivityForecastInfosourceById(UUID id, String username) {
        final PublicUser publicUser = userRepository.findOneByEmail(username);
        if (publicUser == null) {
            throw new ForbiddenException();
        }
        final Infosource infosource = getInfosource(id, publicUser);
        return new InfosourceActivityForecastDetailWithPublicUserDto(infosource);
    }

    private UUID updateInfosource(UUID id, Infosource infosourceToUpdate, PublicUser publicUser) {
        final Infosource infosource = getInfosource(id, publicUser);
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
        final Set<Attachment> attachments = newHashSet(infosourceToUpdate.getAttachments());
        infosource.getAttachments().clear();
        infosource.getAttachments().addAll(attachments);

        infosource.setLastUpdate(now());

        final Infosource savedInfosource = repository.save(infosource);

        // remove scheduling of deletion && schedule new deletion with lastUpdate + 30 days
        schedulerJobService.scheduleAutoDeleteDraftInfosourceJob(savedInfosource);

        return savedInfosource.getId();
    }

    private Infosource getInfosource(UUID id, PublicUser publicUser) {

        Infosource infosource = repository.getReferenceById(id);

        if (infosource.getIssuingPublicUser().getId() != publicUser.getId()) {
            throw new ForbiddenException();
        }
        return infosource;
    }

    public PageImpl<InfosourceTableDto> findAllBySearchCriteria(InfosourceSearchCriteria searchCriteria, Pageable pageable, String username, String lang) {
        final PublicUser publicUser = getPublicUser(username);
        return InfosourceTableUtil.find(entityManager, searchCriteria, pageable, publicUser.getId(), lang);
    }

    @Transactional
    public void send(UUID id, String username) {
        final User user = getPublicUser(username);
        final Infosource infosource = repository.findByIdAndIssuingPublicUserId(id, user.getId()).orElseThrow(NotFoundException::new);

        Set<MaritimeServiceInfosourceRecipientDto> recipients;
        try {
            recipients = routingService.getRecipients(infosource, false);
        } catch (ServiceException e) {
            throw new BadRequestException(e.getMessage());
        }

        final Infosource savedInfosource = sendInfosource(infosource, user);

        List<MaritimeService> recipientsMaritimeService = maritimeServiceRepository.findAllById(
            recipients.stream().map(MaritimeServiceInfosourceRecipientDto::getId).collect(Collectors.toSet())
        );
        recipientService.send(recipientsMaritimeService, savedInfosource, user.getId(), PUBLIC, true);

        // cancel deletion job
        schedulerJobService.removeScheduledAutoDeleteDraftInfosource(savedInfosource);
    }

    public PublicUser getPublicUser(BindingResult bindingResult, String username) throws ServiceException {
        final PublicUser publicUser = getPublicUser(username);

        if (bindingResult.hasErrors()) {
            throw new ServiceException();
        }
        return publicUser;
    }

    public PublicUser getPublicUser(String username) {
        final PublicUser publicUser = userRepository.findOneByEmail(username);
        if (publicUser == null) {
            throw new ForbiddenException();
        }
        return publicUser;
    }

    public InfosourceDetailDto findByIdForDetailsView(UUID id, String username) {
        PublicUser publicUser = userRepository.findOneByEmail(username);
        if (publicUser == null) {
            throw new ForbiddenException();
        }

        Infosource infosource = repository.findById(id).orElseThrow(NotFoundException::new);
        InfosourceDetailDto infosourceDetailDto;

        if (infosource.getNature() == InfosourceNature.BATHY) {
            infosourceDetailDto = new InfosourceBathyDetailDto().fromInfosource(infosource);
        } else {
            infosourceDetailDto = new InfosourceDetailDto().fromInfosource(infosource);
        }

        return infosourceDetailDto;
    }

    @Override
    protected long countReportedInfosourcesByUserWithinYear(User user, int year) {
        return repository.countReportedInfosourcesByPublicUserWithinYear(user.getId(), year);
    }

    @Override
    protected void assignUserAsPublisher(User user, Infosource infosource) {
        final PublicUser publicUser = getPublicUser(user.getEmail());
        infosource.setIssuingPublicUser(publicUser);
    }

    @Override
    @Transactional
    public void delete(UUID id, String username) {
        final User user = getPublicUser(username);
        final Infosource infosource = repository.findByIdAndIssuingPublicUserId(id, user.getId()).orElseThrow(NotFoundException::new);
        if (infosource.getStatus() != DRAFT) {
            throw new BadRequestException("Current infosource status is invalid");
        }

        deleteAttachments(infosource);

        // cancel deletion job
        schedulerJobService.removeScheduledAutoDeleteDraftInfosource(infosource);

        repository.delete(infosource);
    }

    public List<InfosourceServiceStatusServiceUserDto> findServiceStatuses(UUID id, String username) {
        PublicUser publicUser = getPublicUser(username);
        return recipientService.findServiceStatusesForPublicUser(id, publicUser.getId());
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

    @Transactional
    public UUID createInfosourceBathy(Survey survey, PublicUser publicUser) {
        final Infosource infosource = createInfosourceBathyFromSurvey(survey, publicUser)
            .issuingPublicUser(publicUser)
            .build();

        final Infosource savedInfosource = repository.save(infosource);

        // schedule IS draft deletion
        schedulerJobService.scheduleAutoDeleteDraftInfosourceJob(savedInfosource);

        return savedInfosource.getId();
    }

    @Transactional
    public Infosource createInfosourceDeposit(Survey survey, PublicUser publicUser) {
        // DPC_RG_13160
        final Infosource infosource = createInfosourceDepositFromSurvey(survey, publicUser)
            .issuingPublicUser(publicUser)
            .build();
        return repository.save(infosource);
    }

    @Transactional
    public UUID updateBathyReport(UUID id, InfosourceBathyDangerReportCreateWithPublicUserDto dto, BindingResult bindingResult, String username) throws ServiceException {
        checkIfLocalisationIsEmpty(dto.getLocalisation());
        checkBathyGeometries(dto);
        PublicUser publicUser = getPublicUser(bindingResult, username);
        List<Attachment> attachments = attachmentService.findAllByIdIn(dto.getAttachmentIds());
        Infosource infosourceToUpdate = dto.toInfosource(attachments, publicUser);
        return updateInfosource(id, infosourceToUpdate, publicUser);
    }
}
