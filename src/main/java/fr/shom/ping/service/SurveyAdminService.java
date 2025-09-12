package fr.shom.ping.service;

import fr.shom.ping.exception.BadRequestException;
import fr.shom.ping.exception.ForbiddenException;
import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.ServiceUserRole;
import fr.shom.ping.model.Survey;
import fr.shom.ping.model.User;
import fr.shom.ping.model.dto.AbstractSurveyDepositCreateDto;
import fr.shom.ping.model.dto.AbstractSurveyTestCreateDto;
import fr.shom.ping.model.dto.SurveyBriInternalCommentDto;
import fr.shom.ping.model.dto.SurveyCloneDto;
import fr.shom.ping.model.dto.SurveyDepositCreateDtoWithServiceUser;
import fr.shom.ping.model.dto.SurveyDepositEditDtoWithServiceUser;
import fr.shom.ping.model.dto.SurveyModalDtoServiceUser;
import fr.shom.ping.model.dto.SurveyTableBriDto;
import fr.shom.ping.model.dto.SurveyTableDto;
import fr.shom.ping.model.dto.SurveyTestCreateDtoWithServiceUser;
import fr.shom.ping.model.dto.SurveyTestEditDtoWithServiceUser;
import fr.shom.ping.model.enums.MaritimeServiceType;
import fr.shom.ping.model.enums.SurveyState;
import fr.shom.ping.model.predicate.SurveyAdminBriSearchCriteria;
import fr.shom.ping.model.predicate.SurveyAdminSearchCriteria;
import fr.shom.ping.repository.AreaRepository;
import fr.shom.ping.repository.AttachmentRepository;
import fr.shom.ping.repository.InfosourceRepository;
import fr.shom.ping.repository.MaritimeServiceRepository;
import fr.shom.ping.repository.ServiceUserRepository;
import fr.shom.ping.repository.SurveyProcessingResultRepository;
import fr.shom.ping.repository.SurveyRepository;
import fr.shom.ping.scheduling.SchedulerJobService;
import fr.shom.ping.utils.search.SurveyAdminBriTableUtil;
import fr.shom.ping.utils.search.SurveyAdminTableUtil;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

import static fr.shom.ping.model.enums.ServiceRole.SERVICE_ADMIN;
import static fr.shom.ping.model.enums.ServiceRole.SERVICE_OPERATOR;
import static fr.shom.ping.model.enums.ServiceRole.SERVICE_VISITOR;
import static fr.shom.ping.model.enums.SurveyState.TRANSMITTED;
import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Arrays.asList;

@Slf4j
@Service
public class SurveyAdminService extends AbstractSurveyService {

    public static final DateTimeFormatter NUMBER_DATE_PATTERN = ofPattern("uuuuMMdd");

    private final ServiceUserRepository serviceUserRepository;

    private final InfosourceAdminService infosourceAdminService;

    private final ProUserService proUserService;

    private final SurveyArchiveDownloadService archiveDownloadService;

    public SurveyAdminService(EntityManager entityManager, ServiceUserRepository serviceUserRepository, MaritimeServiceRepository maritimeServiceRepository,
        SurveyRepository repository, SurveyProcessingResultRepository surveyProcessingResultRepository, AttachmentService attachmentService,
        AttachmentRepository attachmentRepository, AreaRepository areaRepository, SchedulerJobService schedulerJobService,
        SurveyFileAnalysisService surveyFileAnalysisService, InfosourceRepository infosourceRepository, InfosourceAdminService infosourceAdminService,
        InfosourceRoutingService infosourceRoutingService, ProUserService proUserService, SurveyArchiveDownloadService archiveDownloadService) {
        super(entityManager, serviceUserRepository, maritimeServiceRepository, repository, surveyProcessingResultRepository, attachmentService,
            attachmentRepository, areaRepository, schedulerJobService, surveyFileAnalysisService, infosourceRepository, infosourceRoutingService);
        this.serviceUserRepository = serviceUserRepository;
        this.infosourceAdminService = infosourceAdminService;
        this.proUserService = proUserService;
        this.archiveDownloadService = archiveDownloadService;
    }

    public PageImpl<SurveyTableDto> findAllBySearchCriteria(SurveyAdminSearchCriteria searchCriteria, Pageable pageable, String username) {
        final ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);
        final UUID serviceId = searchCriteria.getIssuingServiceId();
        checkHasRequiredRoleOnService(serviceUser, serviceId);
        return SurveyAdminTableUtil.getInstance().find(entityManager, searchCriteria, pageable);
    }

    public PageImpl<SurveyTableBriDto> findAllBySearchCriteriaForBri(SurveyAdminBriSearchCriteria searchCriteria, Pageable pageable, String username) {
        final ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);
        checkIsBri(serviceUser);
        return SurveyAdminBriTableUtil.getInstance().find(entityManager, searchCriteria, pageable);
    }

    public void checkIsBri(ServiceUser serviceUser) {
        if (!isBri(serviceUser)) {
            throw new ForbiddenException();     // only BRI users should have access
        }
    }

    public boolean isBri(ServiceUser serviceUser) {
        proUserService.setAttributesForEachServiceRole(serviceUser.getRoles());
        final boolean hasNoRequiredRoleOnBriService = serviceUser.getRoles().stream()
            .filter(role -> asList(SERVICE_VISITOR, SERVICE_OPERATOR, SERVICE_ADMIN).contains(role.getServiceRole()))
            .filter(role -> role.getType() == MaritimeServiceType.BRI)
            .noneMatch(ServiceUserRole::getSurveyOwner);
        return !hasNoRequiredRoleOnBriService;
    }

    public void checkHasRequiredRoleOnService(ServiceUser serviceUser, UUID serviceId) {
        final boolean hasNoRequiredRoleOnService = serviceUser.getRoles().stream()
            .filter(serviceUserRole -> asList(SERVICE_OPERATOR, SERVICE_ADMIN).contains(serviceUserRole.getServiceRole()))
            .map(ServiceUserRole::getServiceId)
            .noneMatch(uuid -> uuid.equals(serviceId));
        if (hasNoRequiredRoleOnService) {
            throw new ForbiddenException();
        }
    }

    @Override
    public Survey buildSurvey(AbstractSurveyDepositCreateDto dto, String username) {
        final ServiceUser serviceUser = getUser(username);
        final UUID issuingServiceId = ((SurveyDepositCreateDtoWithServiceUser) dto).getServiceId();
        checkIfServiceUserHasRightOnIssuingService(serviceUser, issuingServiceId);
        final MaritimeService issuingService = maritimeServiceRepository.findById(issuingServiceId).orElseThrow(NotFoundException::new);
        return ((SurveyDepositCreateDtoWithServiceUser) dto).toSurvey(serviceUser, issuingService, getSurveyBuilder(dto));
    }

    @Override
    public Survey buildSurvey(AbstractSurveyTestCreateDto dto, String username) {
        final ServiceUser serviceUser = getUser(username);
        final UUID issuingServiceId = ((SurveyTestCreateDtoWithServiceUser) dto).getServiceId();
        checkIfServiceUserHasRightOnIssuingService(serviceUser, issuingServiceId);
        final MaritimeService issuingService = maritimeServiceRepository.findById(issuingServiceId).orElseThrow(NotFoundException::new);
        return ((SurveyTestCreateDtoWithServiceUser) dto).toSurvey(serviceUser, issuingService, getSurveyBuilder(dto));
    }

    @Override
    public AbstractSurveyDepositCreateDto findDepositById(UUID id, String username) {
        final ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);
        final Survey survey = getSurveyByIdAndStateIn(id, serviceUser, SurveyState.DRAFT, SurveyState.TRANSMITTED);
        return SurveyDepositEditDtoWithServiceUser.from(survey);
    }

    @Override
    public AbstractSurveyTestCreateDto findTestById(UUID id, String username) {
        final ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);
        final Survey survey = getSurveyByIdAndStateIn(id, serviceUser, SurveyState.TEST);
        return SurveyTestEditDtoWithServiceUser.from(survey);
    }

    @Override
    public SurveyModalDtoServiceUser findForDetailsView(UUID id, String username) {
        final ServiceUser serviceUser = getUser(username);
        final Survey survey = getSurvey(id, serviceUser);
        setSurveyAnalysisResult(survey);
        return new SurveyModalDtoServiceUser(survey, isBri(serviceUser));
    }

    @Override
    public Survey getSurveyByIdAndUser(UUID id, User user) {
        return getSurvey(id, (ServiceUser) user);
    }

    @Override
    public void deleteDraftDeposit(UUID id, String username) {
        final ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);
        final Survey survey = getSurvey(id, serviceUser);
        deleteDepositDraft(survey);
    }

    @Override
    public void createAndRouteDepositInfosource(Survey survey) throws ServiceException {
        final Infosource infosource = infosourceAdminService.createInfosourceDeposit(survey, survey.getIssuingServiceUser());
        try {
            infosourceAdminService.send(infosource.getId(), survey.getIssuingServiceUser().getEmail(), getRecipients(infosource));
        } catch (ServiceException e) {
            deleteDepositInfosource(infosource);
            throw e;
        }
    }

    public Survey getSurvey(UUID id, ServiceUser serviceUser) {
        final Survey survey = repository.findById(id).orElseThrow(ForbiddenException::new);
        checkIfServiceUserHasRightOnSurvey(serviceUser, survey);
        return survey;
    }

    public Survey getSurveyByIdAndStateIn(UUID id, ServiceUser serviceUser, SurveyState... states) {
        final Survey survey = repository.findSurveyByIdAndStateIsIn(id, List.of(states)).orElseThrow(ForbiddenException::new);
        checkIfServiceUserHasRightOnSurvey(serviceUser, survey);
        return survey;
    }

    public ServiceUser getUser(String username) {
        return serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);
    }

    public ServiceUser getServiceUser(BindingResult bindingResult, String username) throws ServiceException {
        final ServiceUser serviceUser = getUser(username);

        if (bindingResult.hasErrors()) {
            throw new ServiceException();
        }
        return serviceUser;
    }

    public MaritimeService getService(ServiceUser serviceUser) {
        return maritimeServiceRepository.findById(serviceUser.getService().getId()).orElseThrow(NotFoundException::new);
    }

    @Override
    protected long countCreatedSurveysByUserWithinYear(User user, int year) {
        return repository.countCreatedSurveysByServiceUserWithinYear(user.getId(), year);
    }

    @Transactional
    public UUID clone(UUID id, @Valid SurveyCloneDto dto, String username) {
        final ServiceUser serviceUser = getUser(username);
        final Survey survey = getSurvey(id, serviceUser);
        final Survey clonedSurvey = cloneSurvey(survey, dto)
            .issuingService(Optional.ofNullable(survey.getIssuingService()).orElse(serviceUser.getService()))
            .issuingServiceUser(serviceUser)
            .build();
        final Survey savedSurvey = repository.save(clonedSurvey);
        return savedSurvey.getId();
    }

    public static void checkIfServiceUserHasRightOnIssuingService(ServiceUser serviceUser, UUID issuingServiceId) {
        if (!hasServiceUserRightOnIssuingService(serviceUser, issuingServiceId)) {
            throw new ForbiddenException();
        }
    }

    public static boolean hasServiceUserRightOnIssuingService(ServiceUser serviceUser, UUID issuingServiceId) {
        return serviceUser.getServiceIdsWithOperatorOrAdminRole().contains(issuingServiceId);
    }

    public static void checkIfServiceUserHasRightOnSurvey(ServiceUser serviceUser, Survey survey) {
        // if no issuing service but issuing public user then a service user wants to see a survey created from a public user
        if (survey.getIssuingService() != null && !hasServiceUserRightOnIssuingService(serviceUser, survey.getIssuingService().getId())
            && survey.getIssuingPublicUser() == null) {
            throw new ForbiddenException();
        }
    }

    @Transactional
    public UUID report(UUID id, String username) {
        final ServiceUser serviceUser = getUser(username);
        final Survey survey = getSurvey(id, serviceUser);
        checkIfSurveyCanBeReported(survey);
        return infosourceAdminService.createInfosourceBathy(survey, serviceUser);
    }

    public void updateShomId(UUID surveyId, String shomId) throws ServiceException {
        final Survey survey = repository.findById(surveyId).orElseThrow(NotFoundException::new);
        survey.setShomId(shomId);
        repository.save(survey);
    }

    public void downloadZipArchiveForBri(UUID surveyId, String username, HttpServletResponse response) throws ServiceException {
        final ServiceUser serviceUser = serviceUserRepository.findByEmail(username).orElseThrow(ForbiddenException::new);
        checkIsBri(serviceUser);

        final Survey survey = repository.findById(surveyId).orElseThrow(NotFoundException::new);
        if (survey.getState() != TRANSMITTED) {
            throw new BadRequestException("check.survey.status.not-transmitted.cannot-be-downloaded");
        }
        final LocalDate localDate = LocalDate.ofInstant(survey.getLastUpdateTransmissionDate(), UTC);
        final String filename = localDate.format(NUMBER_DATE_PATTERN) + "_" + survey.getIdentifier() + ".zip";
        response.addHeader("Content-Disposition", "attachment;filename=" + filename);
        archiveDownloadService.generateSurveyZipArchive(survey, response);
    }

    @Transactional
    public UUID updateBriInternalComment(UUID surveyId, SurveyBriInternalCommentDto dto, BindingResult bindingResult, String username) throws ServiceException {
        final ServiceUser serviceUser = getServiceUser(bindingResult, username);
        checkIsBri(serviceUser);
        final Survey survey = repository.findById(surveyId).orElseThrow(NotFoundException::new);
        if (survey.getState() != TRANSMITTED) {
            throw new BadRequestException("check.survey.status.not-transmitted.bri-internal-comment.cannot-be-updated");
        }
        survey.setBriInternalComment(dto.getBriInternalComment());
        return repository.save(survey).getId();
    }
}
