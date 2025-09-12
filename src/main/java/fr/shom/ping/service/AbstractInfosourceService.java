package fr.shom.ping.service;

import com.google.common.collect.Maps;
import fr.shom.ping.exception.BadRequestException;
import fr.shom.ping.exception.ForbiddenException;
import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.model.Attachment;
import fr.shom.ping.model.Feature;
import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.InfosourcePart;
import fr.shom.ping.model.Survey;
import fr.shom.ping.model.SurveyProcessingResult;
import fr.shom.ping.model.User;
import fr.shom.ping.model.dto.FeatureDto;
import fr.shom.ping.model.dto.InfosourceReportAbstractCreateDto;
import fr.shom.ping.model.dto.ReferencesByFolderDto;
import fr.shom.ping.model.enums.SurveyDangerTypeOfValue;
import fr.shom.ping.properties.PingProperties;
import fr.shom.ping.repository.InfosourceRepository;
import fr.shom.ping.repository.MaritimeServiceRepository;
import fr.shom.ping.scheduling.SchedulerJobService;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Geometry;
import org.springframework.transaction.annotation.Transactional;

import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newTreeSet;
import static fr.shom.ping.exception.ThrowingConsumerWrapper.throwingConsumerWrapper;
import static fr.shom.ping.model.enums.InfosourceNature.BATHY;
import static fr.shom.ping.model.enums.InfosourceNature.SURVEY_TRANSMISSION;
import static fr.shom.ping.model.enums.InfosourceStatus.DRAFT;
import static fr.shom.ping.model.enums.InfosourceStatus.SENT;
import static fr.shom.ping.service.SurveyExternalAnalysisService.getLastProcessingResult;
import static java.time.Instant.now;
import static java.time.ZoneOffset.UTC;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public abstract class AbstractInfosourceService {

    protected final EntityManager entityManager;
    protected final InfosourceRepository repository;
    protected final AttachmentService attachmentService;
    protected final InfosourceRoutingService routingService;
    protected final MaritimeServiceRepository maritimeServiceRepository;
    protected final InfosourceRecipientService recipientService;
    protected final SchedulerJobService schedulerJobService;
    protected final FolderService folderService;

    protected final PingProperties pingProperties;

    protected abstract long countReportedInfosourcesByUserWithinYear(User user, int year);

    protected abstract void assignUserAsPublisher(User user, Infosource infosource);

    public abstract void delete(UUID id, String username);

    public String generateInfosourceNumber(User user) {
        final int year = Year.now(UTC).getValue();
        final long numberOfInfosourcesSentInYear = countReportedInfosourcesByUserWithinYear(user, year) + 1;
        return String.format("%s-%02d-%d", user.getIdentifier(), numberOfInfosourcesSentInYear, year);
    }

    @Transactional
    public Infosource sendInfosource(Infosource infosource, User user) {
        if (infosource.getStatus() != DRAFT) {
            throw new BadRequestException("Current infosource status is invalid");
        }

        final String number = generateInfosourceNumber(user);
        Instant now = now();
        infosource.setLastUpdate(now);
        infosource.setValidationTime(now);
        infosource.setStatus(SENT);
        infosource.setNumber(number);
        assignUserAsPublisher(user, infosource);

        return repository.save(infosource);
    }

    @Transactional
    public void autoDelete(UUID infosourceId) {
        final Infosource infosource = repository.findByIdAndDraftStatus(infosourceId).orElseThrow(ForbiddenException::new);
        deleteAttachments(infosource);
        repository.delete(infosource);
    }

    public void deleteAttachments(Infosource infosource) {
        final List<UUID> attachmentList = infosource.getAttachments().stream().map(Attachment::getId).collect(toList());

        // delete files from disk using AttachmentService.delete()
        attachmentList.forEach(throwingConsumerWrapper(this.attachmentService::deleteFromDiskAndDatabase));
    }

    @Transactional
    public Infosource.InfosourceBuilder createInfosourceBathyFromSurvey(Survey survey, User user) {
        final Instant now = now();

        final SurveyProcessingResult processingResult = getLastProcessingResult(survey);

        final Set<Feature> features = processingResult.getDangers().stream().map(surveyDanger -> Feature.builder()
            .geometry(surveyDanger.getGeometry())
            .properties(Maps.newHashMap(Map.of(
                "nature", surveyDanger.getNature(),
                surveyDanger.getTypeOfValue() == SurveyDangerTypeOfValue.DEPTH ? "depth" : "difference", surveyDanger.getValue(),
                "isBathy", true
            )))
            .build()
        ).collect(Collectors.toSet());

        return Infosource.builder()
            .infosourceParts(newTreeSet(
                List.of(InfosourcePart.builder()
                    .position(1)
                    .localisation(features)
                    .description(survey.getNameAndLocation())
                    .build())
            ))
            .nature(BATHY)
            .status(DRAFT)
            .creationDate(now)
            .lastUpdate(now)
            .startDate(survey.getStartDate())
            .endDate(survey.getEndDate())
            .validationTime(now)
            .survey(survey);
    }

    protected Infosource.InfosourceBuilder createInfosourceDepositFromSurvey(Survey survey, User user) {
        final Instant now = now();

        return Infosource.builder()
            .infosourceParts(newTreeSet(
                List.of(InfosourcePart.builder()
                    .position(1)
                    .localisation(newHashSet(Feature.builder().geometry(survey.getBoundingBox()).build()))
                    .description(survey.getNameAndLocation())
                    .build())
            ))
            .nature(SURVEY_TRANSMISSION)
            .status(DRAFT)
            .creationDate(now)
            .lastUpdate(now)
            .validationTime(now)
            .survey(survey);
    }

    protected void checkBathyGeometries(InfosourceReportAbstractCreateDto dto) {
        checkBathyGeometries(dto.getLocalisation());
    }

    protected void checkBathyGeometries(Set<FeatureDto> features) {
        boolean atLeastOneInvalidGeometry = !validateBathyGeometries(features);
        if (atLeastOneInvalidGeometry) {
            throw new BadRequestException("At least one geometry is invalid");
        }
    }

    private boolean validateBathyGeometries(Set<FeatureDto> features) {
        final List<String> allowedGeometryTypes = List.of(Geometry.TYPENAME_POLYGON, Geometry.TYPENAME_POINT);
        return features.stream().map(FeatureDto::getGeometry).map(Geometry::getGeometryType).allMatch(type -> allowedGeometryTypes.contains(type))
            && features.stream().filter(featureDto -> featureDto.getProperties().get("isCircle") == null || featureDto.getProperties().get("isCircle").equals(false))
            .map(FeatureDto::getGeometry)
            .filter(geometry -> Geometry.TYPENAME_POLYGON.equals(geometry.getGeometryType()))
            .map(Geometry::getCoordinates)
            .allMatch(coordinates -> coordinates.length - 1 <= pingProperties.getMaxPolygonVerticesForIsBathy());
    }

    public Set<ReferencesByFolderDto> getReferencesByFolderLinkedToInfosource(UUID infosourceId) {
        final Infosource infosource = repository.findById(infosourceId).orElseThrow(NotFoundException::new);
        final List<UUID> folderIds = infosource.getNotions().stream().map(notionInfosource -> notionInfosource.getFolder().getId()).toList();
        return folderService.findAllReferencesByFolderIds(folderIds);
    }
}
