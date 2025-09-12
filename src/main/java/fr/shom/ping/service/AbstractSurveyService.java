package fr.shom.ping.service;

import com.google.common.collect.ImmutableMap;
import fr.shom.ping.exception.BadRequestException;
import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.Area;
import fr.shom.ping.model.Attachment;
import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.Survey;
import fr.shom.ping.model.SurveyFile;
import fr.shom.ping.model.SurveyFileInArchive;
import fr.shom.ping.model.SurveyProcessingResult;
import fr.shom.ping.model.User;
import fr.shom.ping.model.dto.AbstractSurveyDepositCreateDto;
import fr.shom.ping.model.dto.AbstractSurveyTestCreateDto;
import fr.shom.ping.model.dto.AttachmentSurveyResultDownloadDto;
import fr.shom.ping.model.dto.MaritimeServiceInfosourceRecipientDto;
import fr.shom.ping.model.dto.SurveyAnalysisProgressDto;
import fr.shom.ping.model.dto.SurveyCloneDto;
import fr.shom.ping.model.dto.SurveyDetailDto;
import fr.shom.ping.model.dto.SurveyExternalAnalysisResultsDto;
import fr.shom.ping.model.dto.SurveyFilesExternalAnalysisResultsDto;
import fr.shom.ping.model.enums.SurveyAnalysisProgressStatus;
import fr.shom.ping.model.enums.SurveyFileComplianceAnalysisState;
import fr.shom.ping.model.enums.SurveyProcessingStatus;
import fr.shom.ping.model.enums.SurveyState;
import fr.shom.ping.model.enums.SurveyTransmissionStatus;
import fr.shom.ping.repository.AreaRepository;
import fr.shom.ping.repository.AttachmentRepository;
import fr.shom.ping.repository.InfosourceRepository;
import fr.shom.ping.repository.MaritimeServiceRepository;
import fr.shom.ping.repository.ServiceUserRepository;
import fr.shom.ping.repository.SurveyProcessingResultRepository;
import fr.shom.ping.repository.SurveyRepository;
import fr.shom.ping.scheduling.SchedulerJobService;
import fr.shom.ping.utils.SurveyUtil;
import java.time.Instant;
import java.time.Year;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;

import static com.google.common.collect.Sets.newHashSet;
import static fr.shom.ping.exception.ThrowingConsumerWrapper.throwingConsumerWrapper;
import static fr.shom.ping.model.enums.SurveyProcessingStatus.FAILURE;
import static fr.shom.ping.model.enums.SurveyProcessingStatus.FINISHED;
import static fr.shom.ping.model.enums.SurveyProcessingStatus.NOT_AVAILABLE;
import static fr.shom.ping.model.enums.SurveyProcessingStatus.SUCCESS_WITH_ERRORS;
import static fr.shom.ping.model.enums.SurveyState.DRAFT;
import static fr.shom.ping.model.enums.SurveyState.TEST;
import static fr.shom.ping.model.enums.SurveyState.TRANSMITTED;
import static fr.shom.ping.service.SurveyExternalAnalysisService.getIdFile;
import static fr.shom.ping.service.SurveyExternalAnalysisService.getLastProcessingResult;
import static java.time.Instant.now;
import static java.time.ZoneOffset.UTC;
import static java.util.stream.Collectors.toList;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractSurveyService {
    protected final EntityManager entityManager;

    protected final ServiceUserRepository serviceUserRepository;

    protected final MaritimeServiceRepository maritimeServiceRepository;

    protected final SurveyRepository repository;

    protected final SurveyProcessingResultRepository surveyProcessingResultRepository;

    protected final AttachmentService attachmentService;

    protected final AttachmentRepository attachmentRepository;

    protected final AreaRepository areaRepository;

    protected final SchedulerJobService schedulerJobService;

    protected final SurveyFileAnalysisService surveyFileAnalysisService;

    protected final InfosourceRepository infosourceRepository;

    protected final InfosourceRoutingService infosourceRoutingService;

    protected abstract long countCreatedSurveysByUserWithinYear(User user, int year);

    public abstract AbstractSurveyDepositCreateDto findDepositById(UUID id, String username);

    public abstract AbstractSurveyTestCreateDto findTestById(UUID id, String username);

    public abstract SurveyDetailDto findForDetailsView(UUID id, String username);

    public abstract Survey buildSurvey(AbstractSurveyDepositCreateDto dto, String username);

    public abstract Survey buildSurvey(AbstractSurveyTestCreateDto dto, String username);

    public abstract Survey getSurveyByIdAndUser(UUID id, User user);

    public abstract void deleteDraftDeposit(UUID id, String username);

    public abstract void createAndRouteDepositInfosource(Survey survey) throws ServiceException;

    private static final String MESSAGE = "message";

    @Transactional
    public void autoDelete(UUID surveyId, boolean onlyAttachments) {
        final Survey survey = repository.findById(surveyId).orElseThrow(NotFoundException::new);
        delete(survey, onlyAttachments);
    }

    public void deleteDepositDraft(Survey survey) {
        if (survey.getState() != SurveyState.DRAFT) {
            throw new BadRequestException("Current survey status is invalid: " + survey.getState());
        }

        // delete survey and attachments and jobs
        deleteAllSurveyInfo(survey);
    }

    public void deleteAllSurveyInfo(Survey survey) {
        schedulerJobService.removeScheduledAutoDeleteSurvey(survey);
        delete(survey, false);
    }

    @Transactional
    public void delete(Survey survey, boolean onlyAttachments) {
        deleteAttachments(survey);

        if (!onlyAttachments) {
            repository.delete(survey);
        }
    }

    public void deleteAttachments(Survey survey) {
        final List<UUID> attachmentList = survey.getMiscAttachments().stream()
            .map(Attachment::getId)
            .collect(toList());

        if (survey.getTechnicalMetadataFile() != null) {
            attachmentList.add(survey.getTechnicalMetadataFile().getId());
        }

        survey.getFiles().stream()
            .map(SurveyFile::getAttachment)
            .forEach(throwingConsumerWrapper(this.attachmentService::deleteAttachmentFile));

        // delete files from disk using AttachmentService.delete()
        attachmentList.forEach(throwingConsumerWrapper(this.attachmentService::deleteFromDiskAndDatabase));
    }

    /**
     * delete files from disk using AttachmentService.delete()
     *
     * @param survey                 the warning from db
     * @param updatedSurveyFilesList attachment list to update
     */
    public void deleteUnusedFiles(Survey survey, List<UUID> updatedSurveyFilesList) {
        final List<UUID> formerAttachmentList = survey.getFiles().stream().map(SurveyFile::getAttachment).map(Attachment::getId).collect(toList());
        final List<UUID> deletedAttachments = findDifferences(formerAttachmentList, updatedSurveyFilesList);

        // delete files from disk using AttachmentService.delete()
        deletedAttachments.forEach(throwingConsumerWrapper(this.attachmentService::deleteFromDiskAndDatabase));
    }

    /**
     * delete files from disk using AttachmentService.delete()
     *
     * @param survey                the warning from db
     * @param updatedAttachmentList attachment list to update
     */
    public void deleteUnusedAttachments(Survey survey, List<UUID> updatedAttachmentList) {
        final List<UUID> formerAttachmentList = survey.getMiscAttachments().stream().map(Attachment::getId).collect(toList());
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

    public String generateSurveyIdentifier(User user, SurveyState surveyState) {
        final int year = Year.now(UTC).getValue();
        final long numberOfSurveysInYear = countCreatedSurveysByUserWithinYear(user, year) + 1;
        final String format = (surveyState == TEST ? "T-" : "L-").concat("%s-%02d-%d");
        return String.format(format, user.getIdentifier(), numberOfSurveysInYear, year);
    }

    @Transactional
    public UUID createDeposit(AbstractSurveyDepositCreateDto dto, String username) throws ServiceException {
        return createSurvey(buildSurvey(dto, username), true);
    }

    @Transactional
    public UUID createTest(AbstractSurveyTestCreateDto dto, String username) throws ServiceException {
        return createSurvey(buildSurvey(dto, username), false);
    }

    public UUID createSurvey(Survey survey, boolean deposit) throws ServiceException {
        checkNameAndLocation(survey);
        checkDates(survey);
        checkBathyFiles(survey);

        final Instant now = now();
        survey.setState(deposit ? DRAFT : TEST);
        survey.setCompliant(SurveyFileComplianceAnalysisState.NOT_AVAILABLE);
        survey.setCreationDate(now);
        survey.setLastUpdate(now);

        final String identifier = generateSurveyIdentifier(
            survey.getIssuingServiceUser() != null ? survey.getIssuingServiceUser() : survey.getIssuingPublicUser(), survey.getState()
        );
        survey.setIdentifier(identifier);

        survey.getFiles().forEach(surveyFile -> surveyFile.setCartonautFileId(SurveyUtil.generateBathyFileId()));

        final SurveyProcessingResult surveyProcessingResult = SurveyProcessingResult.builder().processingDate(now()).status(NOT_AVAILABLE).build();
        survey.getProcessingResults().add(surveyProcessingResult);

        final Survey savedSurvey = repository.save(survey);
        repository.flush();

        // remove scheduling of deletion if needed && schedule new deletion with lastUpdate + 30 days
        schedulerJobService.scheduleAutoDeleteSurveyJob(savedSurvey);

        // check files compliant + statistics
        analyzeBathyFiles(savedSurvey);

        return savedSurvey.getId();
    }

    public void analyzeBathyFiles(Survey survey) throws ServiceException {
        surveyFileAnalysisService.analyze(survey);
    }

    private static void checkDates(Survey survey) {
        if (survey.getStartDate() != null && survey.getEndDate() != null && survey.getStartDate().isAfter(survey.getEndDate())) {
            throw new BadRequestException("check.survey.start-date.not-before.end-date");
        }
    }

    private void checkNameAndLocation(Survey survey) {
        if (!repository.isNameAndLocationAvailable(survey.getNameAndLocation())) {
            throw new BadRequestException("check.survey.name-and-location.already-used");
        }
    }

    private static void checkBathyFiles(Survey survey) {
        if (survey.getState() == TEST && survey.getFiles().isEmpty()) {
            throw new BadRequestException("check.survey.test.missing-bathy-file");
        }
    }

    private static void checkUpdatable(Survey survey) {
        if (survey.getState() == TRANSMITTED) {
            throw new BadRequestException("check.survey.status.transmitted.cannot-be-updated");
        }

        if (survey.getState() == TEST) {
            // get last processing result
            final SurveyProcessingResult processingResult = survey.getProcessingResults().stream()
                .max(Comparator.comparing(SurveyProcessingResult::getProcessingDate)).orElseThrow(NotFoundException::new);
            // update survey with info to update externalAnalysisInProgress value before use
            updateSurveyExternalAnalysisInProgress(survey, processingResult);
            if (survey.isExternalAnalysisInProgress()) {
                throw new BadRequestException("check.survey.status.test.cannot-be-updated");
            }
        }
    }

    @Transactional
    public UUID updateDeposit(UUID id, AbstractSurveyDepositCreateDto dto, String username) throws ServiceException {
        return updateSurvey(id, buildSurvey(dto, username), true, false);
    }

    @Transactional
    public UUID updateDeposit(UUID id, AbstractSurveyDepositCreateDto dto, String username, boolean transmit) throws ServiceException {
        return updateSurvey(id, buildSurvey(dto, username), true, transmit);
    }

    @Transactional
    public UUID updateTest(UUID id, AbstractSurveyTestCreateDto dto, String username) throws ServiceException {
        return updateSurvey(id, buildSurvey(dto, username), false, false);
    }

    public UUID updateSurvey(UUID id, Survey surveyToUpdate, boolean deposit, boolean transmit) throws ServiceException {
        checkUpdatable(surveyToUpdate);
        checkDates(surveyToUpdate);
        checkBathyFiles(surveyToUpdate);

        final Survey existingSurvey = getSurveyByIdAndUser(id,
            surveyToUpdate.getIssuingServiceUser() != null ? surveyToUpdate.getIssuingServiceUser() : surveyToUpdate.getIssuingPublicUser());

        if (!StringUtils.equals(surveyToUpdate.getNameAndLocation(), existingSurvey.getNameAndLocation())) {
            checkNameAndLocation(surveyToUpdate);
        }

        final boolean bathyDescriptionOrFilesChanged = checkBathyFilesOrBathyDescriptionChanged(existingSurvey, surveyToUpdate);

        existingSurvey.setNameAndLocation(surveyToUpdate.getNameAndLocation());
        existingSurvey.setArea(surveyToUpdate.getArea());
        existingSurvey.setStartDate(surveyToUpdate.getStartDate());
        existingSurvey.setEndDate(surveyToUpdate.getEndDate());
        existingSurvey.setGeographicFormat(surveyToUpdate.isGeographicFormat());
        existingSurvey.setEpsg(surveyToUpdate.getEpsg());
        existingSurvey.setCoordinatesOrder(surveyToUpdate.getCoordinatesOrder());
        existingSurvey.setZDirection(surveyToUpdate.getZDirection());
        existingSurvey.setVerticalRef(surveyToUpdate.getVerticalRef());
        existingSurvey.setFileSeparator(surveyToUpdate.getFileSeparator());
        existingSurvey.setLastUpdate(now());

        if (deposit) {
            existingSurvey.setSurveyIssuerTypes(surveyToUpdate.getSurveyIssuerTypes());
            existingSurvey.setOwnerOrganization(surveyToUpdate.getOwnerOrganization());
            existingSurvey.setProducerOrganization(surveyToUpdate.getProducerOrganization());
            existingSurvey.setOwnerEmail(surveyToUpdate.getOwnerEmail());
            existingSurvey.setTechnicalManagerEmail(surveyToUpdate.getTechnicalManagerEmail());
            existingSurvey.setGoal(surveyToUpdate.getGoal());
            existingSurvey.setActivityChoice(surveyToUpdate.getActivityChoice());
            existingSurvey.setBroadcastRestriction(surveyToUpdate.getBroadcastRestriction());
            existingSurvey.setConfidentialityInformation(surveyToUpdate.getConfidentialityInformation());
            existingSurvey.setExternalAnalysisEnabled(surveyToUpdate.getExternalAnalysisEnabled());
        }

        if (bathyDescriptionOrFilesChanged) {
            // update survey files
            final Map<UUID, Integer> fileIdMap = existingSurvey.getFiles().stream().filter(surveyFile -> surveyFile.getCartonautFileId() != null)
                .collect(Collectors.toMap(surveyFile -> surveyFile.getAttachment().getId(), SurveyFile::getCartonautFileId));
            final Set<SurveyFile> files = newHashSet(surveyToUpdate.getFiles());
            files.forEach(surveyFile -> {
                Integer cartonautFileId = fileIdMap.get(surveyFile.getAttachment().getId());
                if (cartonautFileId == null) {
                    cartonautFileId = SurveyUtil.generateBathyFileId();
                }
                surveyFile.setCartonautFileId(cartonautFileId);
            });
            existingSurvey.getFiles().clear();
            existingSurvey.getFiles().addAll(files);
        }

        if (deposit) {
            // update misc attachments
            final List<UUID> updatedMiscFileIds = surveyToUpdate.getMiscAttachments().stream()
                .map(Attachment::getId)
                .collect(toList());
            deleteUnusedAttachments(existingSurvey, updatedMiscFileIds);

            final Set<Attachment> misc = newHashSet(surveyToUpdate.getMiscAttachments());

            existingSurvey.getMiscAttachments().clear();
            existingSurvey.getMiscAttachments().addAll(misc);

            // update technical metadata file
            if (existingSurvey.getTechnicalMetadataFile() == null || surveyToUpdate.getTechnicalMetadataFile() != null &&
                surveyToUpdate.getTechnicalMetadataFile().getId() != existingSurvey.getTechnicalMetadataFile().getId()) {
                if (existingSurvey.getTechnicalMetadataFile() != null) {
                    this.attachmentService.deleteFromDiskAndDatabase(existingSurvey.getTechnicalMetadataFile().getId());
                }
                existingSurvey.setTechnicalMetadataFile(surveyToUpdate.getTechnicalMetadataFile());
            }
        }

        final Survey savedSurvey = repository.save(existingSurvey);

        // remove scheduling of deletion if needed && schedule new deletion with lastUpdate + 30 days
        schedulerJobService.scheduleAutoDeleteSurveyJob(savedSurvey);

        // if at least one bathy file has changed or bathy description changed, then new analysis is required
        if (bathyDescriptionOrFilesChanged && !transmit) {
            // check files compliant + statistics
            analyzeBathyFiles(savedSurvey);
        }

        return savedSurvey.getId();
    }

    public static boolean checkBathyFilesOrBathyDescriptionChanged(Survey existingSurvey, Survey surveyToUpdate) {
        final boolean bathyDescriptionChanged = checkBathyDescriptionChanged(existingSurvey, surveyToUpdate);
        if (bathyDescriptionChanged) {
            return true;
        }

        final List<UUID> formerAttachmentList = existingSurvey.getFiles().stream().map(SurveyFile::getAttachment).map(Attachment::getId).collect(toList());
        final List<UUID> newAttachmentList = surveyToUpdate.getFiles().stream().map(SurveyFile::getAttachment).map(Attachment::getId).collect(toList());
        return checkBathyFilesChanged(formerAttachmentList, newAttachmentList);
    }

    public static boolean checkBathyDescriptionChanged(Survey existingSurvey, Survey surveyToUpdate) {
        return existingSurvey.isGeographicFormat() != surveyToUpdate.isGeographicFormat()
            || !StringUtils.equals(existingSurvey.getEpsg(), surveyToUpdate.getEpsg())
            || existingSurvey.getCoordinatesOrder() != surveyToUpdate.getCoordinatesOrder()
            || !StringUtils.equals(existingSurvey.getFileSeparator(), surveyToUpdate.getFileSeparator())
            || existingSurvey.getZDirection() != surveyToUpdate.getZDirection();
    }

    public static boolean checkBathyFilesChanged(List<UUID> formerAttachmentList, List<UUID> newAttachmentList) {
        return formerAttachmentList.size() != newAttachmentList.size()
            || !formerAttachmentList.isEmpty() && !newAttachmentList.isEmpty() && !findDifferences(formerAttachmentList, newAttachmentList).isEmpty();
    }

    public Survey.SurveyBuilder getSurveyBuilder(AbstractSurveyDepositCreateDto dto) {
        final Area area = areaRepository.findById(dto.getAreaId()).orElseThrow(NotFoundException::new);

        final List<Attachment> bathyAttachments = attachmentService.findAllByIdIn(dto.getBathyAttachmentIds());
        if (bathyAttachments.size() != dto.getBathyAttachmentIds().size()) {
            throw new BadRequestException("At least one bathy attachment could not be found");
        }

        final List<Attachment> miscAttachments = attachmentService.findAllByIdIn(dto.getMiscAttachmentIds());
        if (miscAttachments.size() != dto.getMiscAttachmentIds().size()) {
            throw new BadRequestException("At least one miscellaneous attachment could not be found");
        }

        Attachment technicalMetadataFileAttachment = null;
        if (dto.getTechnicalMetadataId() != null) {
            final Optional<Attachment> technicalMetadataFile = attachmentRepository.findById(dto.getTechnicalMetadataId());
            if (technicalMetadataFile.isEmpty()) {
                throw new BadRequestException("Technical metadata file could not be found");
            }
            technicalMetadataFileAttachment = technicalMetadataFile.get();
        }
        return dto.toSurveyBuilder(area, bathyAttachments, miscAttachments, technicalMetadataFileAttachment);
    }

    public Survey.SurveyBuilder getSurveyBuilder(AbstractSurveyTestCreateDto dto) {
        final Area area = areaRepository.findById(dto.getAreaId()).orElseThrow(NotFoundException::new);

        final List<Attachment> bathyAttachments = attachmentService.findAllByIdIn(dto.getBathyAttachmentIds());
        if (bathyAttachments.size() != dto.getBathyAttachmentIds().size()) {
            throw new BadRequestException("At least one bathy attachment could not be found");
        }
        return dto.toSurveyBuilder(area, bathyAttachments);
    }

    public Survey.SurveyBuilder cloneSurvey(Survey survey, @Valid SurveyCloneDto dto) {
        final Instant now = now();
        final Area area = areaRepository.findById(dto.getAreaId()).orElseThrow(NotFoundException::new);
        return Survey.builder()
            .nameAndLocation(dto.getNameAndLocation())
            .area(area)
            .state(survey.getState() == SurveyState.TEST ? SurveyState.TEST : SurveyState.DRAFT)
            .activityChoice(survey.getActivityChoice())
            .confidentialityInformation(survey.getConfidentialityInformation())
            .coordinatesOrder(survey.getCoordinatesOrder())
            .broadcastRestriction(survey.getBroadcastRestriction())
            .epsg(survey.getEpsg())
            .externalAnalysisEnabled(survey.getExternalAnalysisEnabled())
            .fileSeparator(survey.getFileSeparator())
            .geographicFormat(survey.isGeographicFormat())
            .goal(survey.getGoal())
            .identifier(
                generateSurveyIdentifier(
                    survey.getIssuingServiceUser() != null ? survey.getIssuingServiceUser() : survey.getIssuingPublicUser(),
                    survey.getState()
                )
            )
            .metadata(Optional.ofNullable(survey.getMetadata()).map(ImmutableMap::copyOf).orElse(null))
            .surveyIssuerTypes(newHashSet(survey.getSurveyIssuerTypes()))
            .zDirection(survey.getZDirection())
            .verticalRef(survey.getVerticalRef())
            .ownerEmail(survey.getOwnerEmail())
            .ownerOrganization(survey.getOwnerOrganization())
            .producerOrganization(survey.getProducerOrganization())
            .technicalManagerEmail(survey.getTechnicalManagerEmail())
            .compliant(SurveyFileComplianceAnalysisState.NOT_AVAILABLE)
            .creationDate(now)
            .lastUpdate(now)
            .processingResults(newHashSet(SurveyProcessingResult.builder().processingDate(now).status(NOT_AVAILABLE).build()));
    }

    public void analyzeSurveyBathyFiles(UUID surveyId) {
        final Survey survey = repository.findById(surveyId).orElseThrow(NotFoundException::new);
        surveyFileAnalysisService.analyze(survey);
    }

    public static void updateSurveyExternalAnalysisInProgress(Survey survey, SurveyProcessingResult processingResult) {
        if (processingResult.getStatus() != NOT_AVAILABLE) {
            survey.setExternalAnalysisInProgress(processingResult.getExternalAnalysisStatus() != SurveyAnalysisProgressStatus.NOT_STARTED);
        }
    }

    public static void setSurveyAnalysisResult(Survey survey) {
        if (survey.getState() == SurveyState.DRAFT) {
            return;
        }

        // The option must still be available (only after analysis and even no critical probes detected) because the dangerousness of a detection does not
        // result only from the mathematical criteria applied by cartonaut.
        // The user should also consider the location of the detected probe.
        survey.setCanBeReported(List.of(FINISHED, SUCCESS_WITH_ERRORS, FAILURE).contains(getLastProcessingResult(survey).getStatus()));

        final SurveyProcessingResult processingResult = survey.getProcessingResults().stream()
            .max(Comparator.comparing(SurveyProcessingResult::getProcessingDate)).orElseThrow(NotFoundException::new);

        updateSurveyExternalAnalysisInProgress(survey, processingResult);

        if (!List.of(SurveyProcessingStatus.FAILURE, SurveyProcessingStatus.FINISHED, SurveyProcessingStatus.SUCCESS_WITH_ERRORS)
            .contains(processingResult.getStatus())) {
            return;
        }

        SurveyExternalAnalysisResultsDto surveyExternalAnalysisResultsDto = SurveyExternalAnalysisResultsDto.builder().build();
        surveyExternalAnalysisResultsDto.setProcessingStatus(processingResult.getStatus());
        surveyExternalAnalysisResultsDto.setReceptionDate(processingResult.getProcessingDate());
        if (processingResult.getExternalAnalysisStatus() != null) {
            surveyExternalAnalysisResultsDto.setAnalysisProgress(
                SurveyAnalysisProgressDto.builder()
                    .progress(processingResult.getExternalAnalysisProgress())
                    .status(processingResult.getExternalAnalysisStatus().name())
                    .build()
            );
        }

        if (processingResult.getStatus() == SurveyProcessingStatus.FAILURE) {
            if (!processingResult.getErrors().isEmpty()) {
                surveyExternalAnalysisResultsDto.setProcessingResultErrors(processingResult.getErrors().keySet());
            } else if (processingResult.getDetectedObjects().containsKey(MESSAGE)) {
                surveyExternalAnalysisResultsDto.setGlobalErrorMessage(processingResult.getDetectedObjects().get(MESSAGE).toString());
            }

            survey.setSurveyExternalAnalysisResults(surveyExternalAnalysisResultsDto);
            return;
        }

        processingResult.getDetectedObjects().forEach((cartonautFileIdString, errors) -> {
            Integer cartonautFileId = Integer.parseInt(cartonautFileIdString);

            SurveyFile bathyFile = SurveyFile.builder().build();
            SurveyFileInArchive bathyFileInArchive = SurveyFileInArchive.builder().build();

            Optional<SurveyFile> surveyFile = retrieveBathyFileAndUpdateArchive(survey.getFiles(), bathyFileInArchive, cartonautFileId);

            if (surveyFile.isPresent()) {
                bathyFile = surveyFile.get();
            }

            if (bathyFile.getId() != null || bathyFileInArchive.getId() != null) {
                SurveyFilesExternalAnalysisResultsDto surveyFilesExternalAnalysisResultsDto = SurveyFilesExternalAnalysisResultsDto.builder()
                    .filename(bathyFile.isArchive() ? bathyFileInArchive.getFilename() : bathyFile.getAttachment().getFilename())
                    .errors(errors)
                    .build();

                surveyExternalAnalysisResultsDto.getSurveyFilesExternalAnalysisResults().add(surveyFilesExternalAnalysisResultsDto);
            }
        });

        surveyExternalAnalysisResultsDto.setDangers(processingResult.getDangers());
        surveyExternalAnalysisResultsDto.setDetections(processingResult.getDetections());
        surveyExternalAnalysisResultsDto.setEnc(processingResult.getEnc());
        surveyExternalAnalysisResultsDto.setShouldBeReported(lookForValuableProbesInProcessingResults(survey));

        setResultFilesFromProcessingResultAttachments(surveyExternalAnalysisResultsDto.getResultFiles(), processingResult.getAttachments(), survey.getFiles());


        survey.setSurveyExternalAnalysisResults(surveyExternalAnalysisResultsDto);
    }

    private static void setResultFilesFromProcessingResultAttachments(Set<AttachmentSurveyResultDownloadDto> resultFiles,
        List<Attachment> processingResultAttachments,
        Set<SurveyFile> surveyFiles) {
        processingResultAttachments.forEach(attachment -> {
            Integer cartonautFileId = Integer.valueOf(Objects.requireNonNull(getIdFile(attachment.getFilename())));

            SurveyFileInArchive bathyFileInArchive = SurveyFileInArchive.builder().build();

            Optional<SurveyFile> surveyFile = retrieveBathyFileAndUpdateArchive(surveyFiles, bathyFileInArchive, cartonautFileId);

            final SurveyFile bathyFile = surveyFile.orElse(SurveyFile.builder().build());

            if (bathyFile.getId() != null || bathyFileInArchive.getId() != null) {
                resultFiles.add(
                    AttachmentSurveyResultDownloadDto.builder()
                        .id(attachment.getId())
                        .analyzedFileName(bathyFile.isArchive() ? bathyFileInArchive.getFilename() : bathyFile.getAttachment().getFilename())
                        .resultFileName(attachment.getFilename())
                        .totalSize(attachment.getTotalSize())
                        .build()
                );
            }
        });
    }

    private static Optional<SurveyFile> retrieveBathyFileAndUpdateArchive(Set<SurveyFile> surveyFiles, SurveyFileInArchive bathyFileInArchive,
        Integer cartonautFileId) {
        return surveyFiles.stream().filter(file -> {
                if (file.isArchive()) {
                    Optional<SurveyFileInArchive> surveyFileInArchive = file.getFilesInArchive().stream()
                        .filter(fileInArchive -> Objects.equals(fileInArchive.getCartonautFileId(), cartonautFileId))
                        .findFirst();
                    if (surveyFileInArchive.isPresent()) {
                        bathyFileInArchive.setId(surveyFileInArchive.get().getId());
                        bathyFileInArchive.setFilename(surveyFileInArchive.get().getFilename());
                        return true;
                    }
                }

                return Objects.equals(file.getCartonautFileId(), cartonautFileId);
            }
        ).findFirst();
    }

    private static boolean lookForValuableProbesInProcessingResults(Survey survey) {
        final SurveyProcessingResult lastResult = getLastProcessingResult(survey);
        return List.of(FINISHED, SUCCESS_WITH_ERRORS).contains(lastResult.getStatus()) && !lastResult.getDetections().isEmpty();
    }

    protected void checkIfSurveyCanBeReported(Survey survey) {
        final SurveyProcessingResult lastResult = getLastProcessingResult(survey);
        if (!List.of(FINISHED, SUCCESS_WITH_ERRORS, FAILURE).contains(lastResult.getStatus())) {
            throw new BadRequestException("Survey with id " + survey.getId() + " cannot be reported");
        }
    }

    @Async
    public void transmitDepositDraft(UUID surveyId, Boolean externalAnalysisEnabled) throws ServiceException {
        final Survey survey = repository.findById(surveyId).orElseThrow(NotFoundException::new);
        final boolean result = surveyFileAnalysisService.analyzeSync(survey);
        if (result) {
            transmit(survey, externalAnalysisEnabled);
        }
    }

    public void transmit(Survey survey, Boolean externalAnalysisEnabled) throws ServiceException {
        // (1) check survey is a draft and COMPLIANT
        if (survey.getState() != DRAFT) {
            throw new BadRequestException("Survey status is invalid for transmission");
        }

        if (survey.getCompliant() != SurveyFileComplianceAnalysisState.COMPLIANT) {
            throw new BadRequestException("Survey is not compliant for transmission (no bbox available)");
        }

        // (2) update state to transmitted then save
        survey.setLastUpdateTransmissionDate(Instant.now());
        survey.setTransmissionStatus(SurveyTransmissionStatus.PENDING);
        survey.setExternalAnalysisEnabled(externalAnalysisEnabled);
        final Survey savedSurvey = repository.save(survey);

        if (surveyFileAnalysisService.canBeAnalyzedByExternalProcess(savedSurvey)) {
            // prepare files to be provided to SFTP
            try {
                surveyFileAnalysisService.prepareFilesForExternalAnalysis(savedSurvey);
            } catch (ServiceException e) {
                log.error("Failed to prepare files for external analysis", e);
            }
        }

        // (3) create deposit IS immediately with data from survey (such as bounding box) and route IS to concerned services
        try {
            processTransmittedSurvey(savedSurvey, SurveyTransmissionStatus.FINISHED);

            // remove scheduling of deletion if needed && schedule new deletion with lastUpdate + 60 days
            schedulerJobService.scheduleAutoDeleteSurveyJob(savedSurvey);
        } catch (ServiceException | BadRequestException e) {
            log.error("Failed to create and route deposit IS", e);
            savedSurvey.setState(DRAFT);
            savedSurvey.setTransmissionStatus(SurveyTransmissionStatus.FAILURE);
            repository.save(savedSurvey);
            throw e;
        }
    }

    protected void processTransmittedSurvey(Survey survey, SurveyTransmissionStatus transmissionStatus)
        throws ServiceException {
        // DPC_RG_13163
        createAndRouteDepositInfosource(survey);

        survey.setState(TRANSMITTED);
        survey.setLastUpdate(Instant.now());
        survey.setLastUpdateTransmissionDate(Instant.now());
        survey.setTransmissionStatus(transmissionStatus);
        repository.save(survey);
    }

    protected Set<String> getRecipients(Infosource infosource) throws ServiceException {
        final Set<MaritimeServiceInfosourceRecipientDto> recipients = infosourceRoutingService.getRecipients(infosource, false);
        return recipients.stream().map(MaritimeServiceInfosourceRecipientDto::getId)
            .map(UUID::toString)
            .collect(Collectors.toSet());
    }

    @Transactional
    public void deleteDepositInfosource(Infosource infosource) {
        infosourceRepository.delete(infosource);
    }
}
