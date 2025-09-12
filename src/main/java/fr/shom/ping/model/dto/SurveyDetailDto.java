package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.FixedDateRange;
import fr.shom.ping.model.Survey;
import fr.shom.ping.model.SurveyFile;
import fr.shom.ping.model.enums.SurveyActivity;
import fr.shom.ping.model.enums.SurveyBroadcastRestriction;
import fr.shom.ping.model.enums.SurveyCoordinatesOrder;
import fr.shom.ping.model.enums.SurveyFileComplianceAnalysisState;
import fr.shom.ping.model.enums.SurveyFileStatisticFields;
import fr.shom.ping.model.enums.SurveyIssuerType;
import fr.shom.ping.model.enums.SurveyState;
import fr.shom.ping.model.enums.SurveyVerticalRef;
import fr.shom.ping.model.enums.SurveyZDirection;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.locationtech.jts.geom.Geometry;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.google.common.collect.Sets.newHashSet;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Survey Deposit Create Dto Model")
public class SurveyDetailDto extends AbstractSurveyDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 603468815346366660L;

    protected String briInternalComment;

    protected SurveyBroadcastRestriction broadcastRestriction;

    protected SurveyActivity activityChoice;

    protected String confidentialityInformation;

    @Builder.Default
    protected Set<SurveyIssuerType> surveyIssuerTypes = newHashSet();

    protected String goal;

    protected SurveyState state;

    protected String ownerOrganization;

    protected String ownerEmail;

    protected Boolean geographicFormat;

    protected UUID areaId;

    protected FixedDateRange achievementPeriod;

    protected String epsg;

    protected SurveyCoordinatesOrder coordinatesOrder;

    protected SurveyZDirection directionZ;

    protected SurveyVerticalRef verticalRef;

    protected String fileSeparator;

    protected Boolean externalAnalysisEnabled;

    protected String producerOrganization;

    protected String technicalManagerEmail;

    protected UUID technicalMetadataId;

    @Builder.Default
    protected List<UUID> miscAttachmentIds = new ArrayList<>();

    @Builder.Default
    protected List<SurveyFile> bathyAttachmentFiles = new ArrayList<>();

    protected Instant creationDate;

    protected String areaName;

    protected String issuingUserFirstName;

    protected String issuingUserLastName;

    protected String issuingServiceName;

    protected AttachmentSurveyModalDto technicalMetadataFile;

    @Builder.Default
    protected Set<AttachmentSurveyModalDto> miscAttachmentFiles = newHashSet();

    @Builder.Default
    protected Map<SurveyFileStatisticFields, String> statistics = new HashMap<>();

    @Builder.Default
    protected SurveyFileComplianceAnalysisState compliant = SurveyFileComplianceAnalysisState.NOT_AVAILABLE;

    protected SurveyExternalAnalysisResultsDto surveyExternalAnalysisResults;

    @Builder.Default
    protected boolean externalAnalysisInProgress = false;

    @Builder.Default
    protected boolean canBeReported = false;

    protected Geometry boundingBox;

    public SurveyDetailDto(Survey survey) {
        this.id = survey.getId();
        this.state = survey.getState();
        this.startDate = survey.getStartDate();
        this.endDate = survey.getEndDate();
        this.nameAndLocation = survey.getNameAndLocation();
        this.areaName = survey.getArea().getName();
        this.goal = survey.getGoal();
        this.geographicFormat = survey.isGeographicFormat();
        this.epsg = survey.getEpsg();
        this.coordinatesOrder = survey.getCoordinatesOrder();
        this.activityChoice = survey.getActivityChoice();
        this.broadcastRestriction = survey.getBroadcastRestriction();
        this.confidentialityInformation = survey.getConfidentialityInformation();
        this.directionZ = survey.getZDirection();
        this.verticalRef = survey.getVerticalRef();
        this.fileSeparator = survey.getFileSeparator();
        this.externalAnalysisEnabled = survey.getExternalAnalysisEnabled();
        this.creationDate = survey.getCreationDate();
        this.lastUpdate = survey.getLastUpdate();
        this.bathyAttachmentFiles = new ArrayList<>(survey.getFiles());
        if (isNotEmpty(survey.getTechnicalMetadataFile())) {
            this.technicalMetadataFile = AttachmentSurveyModalDto.fromAttachment(survey.getTechnicalMetadataFile());
        }
        this.miscAttachmentFiles = survey.getMiscAttachments().stream().map(AttachmentSurveyModalDto::fromAttachment).collect(Collectors.toSet());
        this.surveyIssuerTypes = survey.getSurveyIssuerTypes();
        this.ownerOrganization = survey.getOwnerOrganization();
        this.ownerEmail = survey.getOwnerEmail();
        this.producerOrganization = survey.getProducerOrganization();
        this.technicalManagerEmail = survey.getTechnicalManagerEmail();
        this.statistics = survey.getStatistics();
        this.compliant = survey.getCompliant();
        this.surveyExternalAnalysisResults = survey.getSurveyExternalAnalysisResults();
        this.externalAnalysisInProgress = survey.isExternalAnalysisInProgress();
        this.canBeReported = survey.isCanBeReported();
        this.boundingBox = survey.getBoundingBox();
    }
}
