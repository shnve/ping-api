package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Attachment;
import fr.shom.ping.model.FixedDateRange;
import fr.shom.ping.model.Survey;
import fr.shom.ping.model.SurveyFile;
import fr.shom.ping.model.enums.SurveyActivity;
import fr.shom.ping.model.enums.SurveyBroadcastRestriction;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static java.time.ZoneOffset.UTC;
import static java.util.stream.Collectors.toList;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
@NoArgsConstructor
@JsonInclude(NON_NULL)
@SuperBuilder
@Schema(name = "Survey Deposit Edit Dto Model with public user")
public class SurveyDepositEditDtoWithPublicUser extends SurveyDepositCreateDtoWithPublicUser {
    @Serial
    private static final long serialVersionUID = -2492621988604346018L;

    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @Schema(description = "technical metadata file (PDF)", required = true)
    private AttachmentDownloadDto technicalMetadataFile;

    @Builder.Default
    private List<AttachmentDownloadDto> bathyAttachmentFiles = new ArrayList<>();

    @Builder.Default
    private List<AttachmentDownloadDto> miscAttachmentFiles = new ArrayList<>();

    public static SurveyDepositEditDtoWithPublicUser from(Survey survey) {
        return SurveyDepositEditDtoWithPublicUser.builder()
            .id(survey.getId())
            .nameAndLocation(survey.getNameAndLocation())
            .areaId(survey.getArea().getId())
            .goal(survey.getGoal())
            .achievementPeriod(
                survey.getStartDate() != null && survey.getEndDate() != null ?
                    FixedDateRange.builder()
                        .dateStart(survey.getStartDate().atZone(UTC).toLocalDate())
                        .dateEnd(survey.getEndDate().atZone(UTC).toLocalDate())
                        .build()
                    : null
            )
            .geographicFormat(survey.isGeographicFormat())
            .epsg(survey.getEpsg())
            .coordinatesOrder(survey.getCoordinatesOrder())
            .activityChoice(Optional.ofNullable(survey.getActivityChoice()).map(SurveyActivity::name).orElse(null))
            .broadcastRestriction(Optional.ofNullable(survey.getBroadcastRestriction()).map(SurveyBroadcastRestriction::name).orElse(null))
            .confidentialityInformation(survey.getConfidentialityInformation())
            .directionZ(survey.getZDirection())
            .verticalRef(survey.getVerticalRef())
            .fileSeparator(survey.getFileSeparator())
            .externalAnalysisEnabled(survey.getExternalAnalysisEnabled())
            .bathyAttachmentFiles(
                survey.getFiles().stream()
                    .map(SurveyFile::getAttachment)
                    .map(attachment -> AttachmentDownloadDto.builder()
                        .id(attachment.getId())
                        .filename(attachment.getFilename())
                        .totalSize(attachment.getTotalSize())
                        .build())
                    .collect(toList())
            )
            .miscAttachmentFiles(
                survey.getMiscAttachments().stream()
                    .map(attachment -> AttachmentDownloadDto.builder()
                        .id(attachment.getId())
                        .filename(attachment.getFilename())
                        .totalSize(attachment.getTotalSize())
                        .build())
                    .collect(toList())
            )
            .technicalMetadataFile(survey.getTechnicalMetadataFile() == null ? null :
                AttachmentDownloadDto.builder()
                    .id(survey.getTechnicalMetadataFile().getId())
                    .filename(survey.getTechnicalMetadataFile().getFilename())
                    .totalSize(survey.getTechnicalMetadataFile().getTotalSize())
                    .build())
            .surveyIssuerTypes(survey.getSurveyIssuerTypes())
            .ownerOrganization(survey.getOwnerOrganization())
            .ownerEmail(survey.getOwnerEmail())
            .producerOrganization(survey.getProducerOrganization())
            .technicalManagerEmail(survey.getTechnicalManagerEmail())
            .technicalMetadataId(survey.getTechnicalMetadataFile() == null ? null : survey.getTechnicalMetadataFile().getId())
            .bathyAttachmentIds(survey.getFiles().stream()
                .map(SurveyFile::getAttachment)
                .map(Attachment::getId)
                .collect(toList())
            )
            .miscAttachmentIds(survey.getMiscAttachments().stream().map(Attachment::getId).collect(toList()))
            .compliant(survey.getCompliant())
            .transmissionStatus(survey.getTransmissionStatus())
            .build();
    }
}
