package fr.shom.ping.model.dto;

import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.SurveyDetection;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import jakarta.validation.Valid;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import static com.google.common.collect.Sets.newHashSet;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static java.util.stream.Collectors.toList;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
@NoArgsConstructor
@SuperBuilder
@Schema(name = "Infosource Detail Dto (Bathy Danger Report) (Service User)")
public class InfosourceBathyDangerReportDetailWithServiceUserDto extends InfosourceBathyDangerReportCreateWithServiceUserDto {

    @Serial
    private static final long serialVersionUID = -1484422893891113992L;

    @Schema(description = ID_DESCRIPTION, requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID id;

    @Builder.Default
    private Set<SurveyDetectionDto> surveyDetections = newHashSet();

    @Builder.Default
    public List<@Valid AttachmentDownloadDto> attachmentFiles = new ArrayList<>();

    public InfosourceBathyDangerReportDetailWithServiceUserDto(Infosource infosource, Set<SurveyDetection> detections) {
        super(infosource);
        this.id = infosource.getId();
        surveyDetections = SurveyDetectionDto.mapToSurveyDetectionDto(detections);
        this.attachmentFiles = infosource.getAttachments()
            .stream()
            .map(AttachmentDownloadDto::new)
            .collect(toList());
    }
}
