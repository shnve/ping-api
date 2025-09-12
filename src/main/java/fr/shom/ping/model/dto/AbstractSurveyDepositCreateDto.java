package fr.shom.ping.model.dto;

import fr.shom.ping.model.Area;
import fr.shom.ping.model.Attachment;
import fr.shom.ping.model.FixedDateRange;
import fr.shom.ping.model.Survey;
import fr.shom.ping.model.enums.SurveyActivity;
import fr.shom.ping.model.enums.SurveyAnalysisProgressStatus;
import fr.shom.ping.model.enums.SurveyBroadcastRestriction;
import fr.shom.ping.model.enums.SurveyFileComplianceAnalysisState;
import fr.shom.ping.model.enums.SurveyIssuerType;
import fr.shom.ping.model.enums.SurveyTransmissionStatus;
import fr.shom.ping.validation.ValueOfEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static com.google.common.collect.Sets.newHashSet;
import static fr.shom.ping.utils.PingStringUtil.REGEX_PATTERN_NULL_OR_NOT_BLANK;
import static java.time.ZoneOffset.UTC;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@SuperBuilder
@Schema(name = "Survey Deposit Create Dto Model")
public abstract class AbstractSurveyDepositCreateDto extends AbstractSurveyCreateDto {
    @Serial
    private static final long serialVersionUID = 44281458439758014L;

    @Pattern(regexp = REGEX_PATTERN_NULL_OR_NOT_BLANK, flags = { Pattern.Flag.DOTALL }, message = "validation.survey.goal.null-or-not-blank")
    protected String goal;

    @Schema(description = "Achievement period")
    protected FixedDateRange achievementPeriod;

    @Builder.Default
    protected Set<SurveyIssuerType> surveyIssuerTypes = newHashSet();

    @Pattern(regexp = REGEX_PATTERN_NULL_OR_NOT_BLANK, message = "validation.survey.activity-choice.null-or-not-blank")
    @ValueOfEnum(enumClass = SurveyActivity.class, message = "validation.survey.activity-choice.not-in-enum")
    protected String activityChoice;

    @Pattern(regexp = REGEX_PATTERN_NULL_OR_NOT_BLANK, message = "validation.survey.broadcast-restriction.null-or-not-blank")
    @ValueOfEnum(enumClass = SurveyBroadcastRestriction.class, message = "validation.survey.broadcast-restriction.not-in-enum")
    protected String broadcastRestriction;

    @Pattern(regexp = REGEX_PATTERN_NULL_OR_NOT_BLANK, flags = { Pattern.Flag.DOTALL }, message = "validation.survey.confidentiality-information.null-or-not-blank")
    protected String confidentialityInformation;

    @Pattern(regexp = REGEX_PATTERN_NULL_OR_NOT_BLANK, message = "validation.survey.owner-organization.null-or-not-blank")
    protected String ownerOrganization;

    @Pattern(regexp = REGEX_PATTERN_NULL_OR_NOT_BLANK, message = "validation.survey.owner-email.null-or-not-blank")
    protected String ownerEmail;

    @Pattern(regexp = REGEX_PATTERN_NULL_OR_NOT_BLANK, message = "validation.survey.producer-organization.null-or-not-blank")
    protected String producerOrganization;

    @Pattern(regexp = REGEX_PATTERN_NULL_OR_NOT_BLANK, message = "validation.survey.technical-manager-email.null-or-not-blank")
    protected String technicalManagerEmail;

    protected UUID technicalMetadataId;

    @Builder.Default
    protected List<UUID> miscAttachmentIds = new ArrayList<>();

    @Builder.Default
    protected SurveyFileComplianceAnalysisState compliant = SurveyFileComplianceAnalysisState.NOT_AVAILABLE;

    @Builder.Default
    protected SurveyTransmissionStatus transmissionStatus = SurveyTransmissionStatus.NOT_AVAILABLE;

    public Survey.SurveyBuilder toSurveyBuilder(Area area, List<Attachment> bathyAttachments,
        List<Attachment> miscAttachments, Attachment technicalMetadataFile) {
        return super.toSurveyBuilder(area, bathyAttachments)
            .goal(goal)
            .startDate(
                achievementPeriod != null && achievementPeriod.getDateStart() != null ? achievementPeriod.getDateStart().atStartOfDay().toInstant(UTC) : null
            )
            .endDate(
                achievementPeriod != null && achievementPeriod.getDateEnd() != null ? achievementPeriod.getDateEnd().atStartOfDay().toInstant(UTC) : null
            )
            .surveyIssuerTypes(surveyIssuerTypes != null ? new HashSet<>(surveyIssuerTypes) : newHashSet())
            .activityChoice(Optional.ofNullable(activityChoice).map(SurveyActivity::valueOf).orElse(null))
            .broadcastRestriction(Optional.ofNullable(broadcastRestriction).map(SurveyBroadcastRestriction::valueOf).orElse(null))
            .confidentialityInformation(confidentialityInformation)
            .ownerOrganization(ownerOrganization)
            .ownerEmail(ownerEmail)
            .producerOrganization(producerOrganization)
            .technicalManagerEmail(technicalManagerEmail)
            .technicalMetadataFile(technicalMetadataFile)
            .miscAttachments(newHashSet(miscAttachments))
            .externalAnalysisEnabled(externalAnalysisEnabled)
            .compliant(compliant)
            .transmissionStatus(transmissionStatus);
    }
}
