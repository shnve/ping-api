package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Attachment;
import fr.shom.ping.model.FixedDateRange;
import fr.shom.ping.model.Survey;
import fr.shom.ping.model.SurveyFile;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
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
@Schema(name = "Survey Test Edit Dto Model with service user")
public class SurveyTestEditDtoWithServiceUser extends SurveyTestCreateDtoWithServiceUser {
    @Serial
    private static final long serialVersionUID = 8501121454321130239L;

    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @Builder.Default
    private List<AttachmentDownloadDto> bathyAttachmentFiles = new ArrayList<>();

    public static SurveyTestEditDtoWithServiceUser from(Survey survey) {
        final SurveyTestEditDtoWithServiceUser.SurveyTestEditDtoWithServiceUserBuilder builder = SurveyTestEditDtoWithServiceUser.builder()
            .id(survey.getId())
            .nameAndLocation(survey.getNameAndLocation())
            .areaId(survey.getArea().getId())
            .geographicFormat(survey.isGeographicFormat())
            .epsg(survey.getEpsg())
            .coordinatesOrder(survey.getCoordinatesOrder())
            .directionZ(survey.getZDirection())
            .verticalRef(survey.getVerticalRef())
            .fileSeparator(survey.getFileSeparator())
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
            .bathyAttachmentIds(survey.getFiles().stream()
                .map(SurveyFile::getAttachment)
                .map(Attachment::getId)
                .collect(toList())
            );

        if (survey.getStartDate() != null && survey.getEndDate() != null) {
            builder.achievementPeriod(
                FixedDateRange.builder()
                    .dateStart(survey.getStartDate().atZone(UTC).toLocalDate())
                    .dateEnd(survey.getEndDate().atZone(UTC).toLocalDate())
                    .build()
            );
        }

        return builder.build();
    }
}
