package fr.shom.ping.model.dto;

import fr.shom.ping.model.Area;
import fr.shom.ping.model.Attachment;
import fr.shom.ping.model.Survey;
import fr.shom.ping.model.SurveyFile;
import fr.shom.ping.model.enums.SurveyCoordinatesOrder;
import fr.shom.ping.model.enums.SurveyVerticalRef;
import fr.shom.ping.model.enums.SurveyZDirection;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static fr.shom.ping.model.enums.SurveyFileComplianceAnalysisState.NOT_AVAILABLE;
import static fr.shom.ping.utils.PingStringUtil.REGEX_PATTERN_NULL_OR_NOT_BLANK;
import static java.util.stream.Collectors.toSet;

@Data
@NoArgsConstructor
@SuperBuilder
@Schema(name = "Survey Deposit Create Dto")
public abstract class AbstractSurveyCreateDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -8879908077504298066L;

    @NotEmpty(message = "validation.survey.name-and-location.not-empty")
    @NotBlank(message = "validation.survey.name-and-location.not-blank")
    protected String nameAndLocation;

    @NotNull(message = "validation.survey.area-id.not-null")
    protected UUID areaId;

    @Pattern(regexp = REGEX_PATTERN_NULL_OR_NOT_BLANK, flags = { Pattern.Flag.DOTALL }, message = "validation.survey.internal-comment.null-or-not-blank")
    protected String internalComment;

    @NotNull(message = "validation.survey.geographic-format.not-null")
    protected Boolean geographicFormat;

    @Schema(description = "Epsg code")
    @Pattern(regexp = REGEX_PATTERN_NULL_OR_NOT_BLANK, message = "validation.survey.epsg.null-or-not-blank")
    protected String epsg;

    protected SurveyCoordinatesOrder coordinatesOrder;

    protected SurveyZDirection directionZ;

    protected SurveyVerticalRef verticalRef;

    @NotEmpty(message = "validation.survey.file-separator.null-or-not-blank")
    protected String fileSeparator;

    @Builder.Default
    protected List<UUID> bathyAttachmentIds = new ArrayList<>();

    @Builder.Default
    protected Boolean externalAnalysisEnabled = false;

    public Survey.SurveyBuilder toSurveyBuilder(Area area, List<Attachment> bathyAttachments) {
        return Survey.builder()
            .nameAndLocation(nameAndLocation)
            .area(area)
            .geographicFormat(geographicFormat)
            .epsg(epsg)
            .coordinatesOrder(coordinatesOrder)
            .zDirection(directionZ)
            .verticalRef(verticalRef)
            .fileSeparator(fileSeparator)
            .externalAnalysisEnabled(externalAnalysisEnabled)
            .files(bathyAttachments.stream().map(attachment ->
                SurveyFile.builder().compliant(NOT_AVAILABLE).attachment(attachment).build()).collect(toSet())
            );
    }
}
