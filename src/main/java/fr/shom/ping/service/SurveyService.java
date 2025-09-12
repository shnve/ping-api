package fr.shom.ping.service;

import fr.shom.ping.exception.ForbiddenException;
import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.PublicUser;
import fr.shom.ping.model.Survey;
import fr.shom.ping.model.User;
import fr.shom.ping.model.dto.AbstractSurveyDepositCreateDto;
import fr.shom.ping.model.dto.AbstractSurveyTestCreateDto;
import fr.shom.ping.model.dto.SurveyCloneDto;
import fr.shom.ping.model.dto.SurveyDepositCreateDtoWithPublicUser;
import fr.shom.ping.model.dto.SurveyDepositEditDtoWithPublicUser;
import fr.shom.ping.model.dto.SurveyModalDtoPublicUser;
import fr.shom.ping.model.dto.SurveyTableDto;
import fr.shom.ping.model.dto.SurveyTestCreateDtoWithPublicUser;
import fr.shom.ping.model.dto.SurveyTestEditDtoWithPublicUser;
import fr.shom.ping.model.enums.SurveyState;
import fr.shom.ping.model.predicate.SurveySearchCriteria;
import fr.shom.ping.repository.AreaRepository;
import fr.shom.ping.repository.AttachmentRepository;
import fr.shom.ping.repository.InfosourceRepository;
import fr.shom.ping.repository.MaritimeServiceRepository;
import fr.shom.ping.repository.ServiceUserRepository;
import fr.shom.ping.repository.SurveyProcessingResultRepository;
import fr.shom.ping.repository.SurveyRepository;
import fr.shom.ping.repository.UserRepository;
import fr.shom.ping.scheduling.SchedulerJobService;
import fr.shom.ping.utils.search.SurveyTableUtil;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class SurveyService extends AbstractSurveyService {

    private final UserRepository userRepository;

    private final InfosourceSecuredService infosourceSecuredService;

    public SurveyService(EntityManager entityManager, ServiceUserRepository serviceUserRepository, MaritimeServiceRepository maritimeServiceRepository,
        SurveyRepository repository, SurveyProcessingResultRepository surveyProcessingResultRepository, AttachmentService attachmentService,
        AttachmentRepository attachmentRepository, AreaRepository areaRepository, SchedulerJobService schedulerJobService, UserRepository userRepository,
        SurveyFileAnalysisService surveyFileAnalysisService, InfosourceSecuredService infosourceSecuredService, InfosourceRepository infosourceRepository,
        InfosourceSecuredService infosourceService, InfosourceRoutingService infosourceRoutingService) {
        super(entityManager, serviceUserRepository, maritimeServiceRepository, repository, surveyProcessingResultRepository, attachmentService,
            attachmentRepository, areaRepository, schedulerJobService, surveyFileAnalysisService, infosourceRepository, infosourceRoutingService);
        this.userRepository = userRepository;
        this.infosourceSecuredService = infosourceSecuredService;
    }

    public PageImpl<SurveyTableDto> findAllBySearchCriteria(SurveySearchCriteria searchCriteria, Pageable pageable, String username) {
        final PublicUser publicUser = userRepository.findOneByEmail(username);
        if (publicUser == null) {
            throw new ForbiddenException();
        }
        return SurveyTableUtil.getInstance().find(entityManager, searchCriteria, pageable, publicUser.getId());
    }

    @Override
    public AbstractSurveyDepositCreateDto findDepositById(UUID id, String username) {
        final PublicUser publicUser = userRepository.findOneByEmail(username);
        if (publicUser == null) {
            throw new ForbiddenException();
        }
        final Survey survey = getSurveyByIdAndStateIn(id, publicUser, SurveyState.DRAFT, SurveyState.TRANSMITTED);
        return SurveyDepositEditDtoWithPublicUser.from(survey);
    }

    @Override
    public SurveyModalDtoPublicUser findForDetailsView(UUID id, String username) {
        final PublicUser publicUser = getUser(username);
        final Survey survey = getSurvey(id, publicUser);
        setSurveyAnalysisResult(survey);
        return new SurveyModalDtoPublicUser(survey);
    }

    @Override
    public AbstractSurveyTestCreateDto findTestById(UUID id, String username) {
        final PublicUser publicUser = userRepository.findOneByEmail(username);
        if (publicUser == null) {
            throw new ForbiddenException();
        }
        final Survey survey = getSurveyByIdAndStateIn(id, publicUser, SurveyState.TEST);
        return SurveyTestEditDtoWithPublicUser.from(survey);
    }

    @Override
    public Survey getSurveyByIdAndUser(UUID id, User user) {
        return getSurvey(id, (PublicUser) user);
    }

    @Override
    public void deleteDraftDeposit(UUID id, String username) {
        final PublicUser publicUser = userRepository.findOneByEmail(username);
        if (publicUser == null) {
            throw new ForbiddenException();
        }
        final Survey survey = getSurvey(id, publicUser);
        deleteDepositDraft(survey);
    }

    @Override
    public void createAndRouteDepositInfosource(Survey survey) {
        final Infosource infosource = infosourceSecuredService.createInfosourceDeposit(survey, survey.getIssuingPublicUser());
        infosourceSecuredService.send(infosource.getId(), survey.getIssuingPublicUser().getEmail());
    }

    public Survey getSurvey(UUID id, PublicUser publicUser) {
        final Survey survey = repository.findById(id).orElseThrow(ForbiddenException::new);

        if (survey.getIssuingPublicUser().getId() != publicUser.getId()) {
            throw new ForbiddenException();
        }
        return survey;
    }

    public Survey getSurveyByIdAndStateIn(UUID id, PublicUser publicUser, SurveyState... states) {
        final Survey survey = repository.findSurveyByIdAndStateIsIn(id, List.of(states)).orElseThrow(ForbiddenException::new);

        if (survey.getIssuingPublicUser().getId() != publicUser.getId()) {
            throw new ForbiddenException();
        }
        return survey;
    }

    @Override
    protected long countCreatedSurveysByUserWithinYear(User user, int year) {
        return repository.countCreatedSurveysByPublicUserWithinYear(user.getId(), year);
    }

    @Override
    public Survey buildSurvey(AbstractSurveyDepositCreateDto dto, String username) {
        return ((SurveyDepositCreateDtoWithPublicUser) dto).toSurvey(getUser(username), getSurveyBuilder(dto));
    }

    @Override
    public Survey buildSurvey(AbstractSurveyTestCreateDto dto, String username) {
        return ((SurveyTestCreateDtoWithPublicUser) dto).toSurvey(getUser(username), getSurveyBuilder(dto));
    }

    public PublicUser getUser(String username) {
        final PublicUser publicUser = userRepository.findOneByEmail(username);
        if (publicUser == null) {
            throw new ForbiddenException();
        }
        return publicUser;
    }

    @Transactional
    public UUID clone(UUID id, @Valid SurveyCloneDto dto, String username) {
        final PublicUser user = getUser(username);
        final Survey survey = getSurvey(id, user);
        final Survey clonedSurvey = cloneSurvey(survey, dto)
            .issuingPublicUser(user)
            .build();
        final Survey savedSurvey = repository.save(clonedSurvey);
        return savedSurvey.getId();
    }

    @Transactional
    public UUID report(UUID id, String username) {
        final PublicUser publicUser = getUser(username);
        final Survey survey = getSurvey(id, publicUser);
        checkIfSurveyCanBeReported(survey);
        return infosourceSecuredService.createInfosourceBathy(survey, publicUser);
    }
}
