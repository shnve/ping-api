package fr.shom.ping.model.dto;

import fr.shom.ping.model.enums.SurveyAnalysisProgressStatus;
import fr.shom.ping.model.enums.SurveyProcessingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Survey analysis progress Dto Model")
public class SurveyAnalysisProgressDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 2521068609206683232L;

    @NotBlank(message = "validation.survey.status.not-empty")
    @Pattern(regexp = "started|download|analysing")
    private String status;

    @NotNull(message = "validation.survey.progress.not-null")
    @DecimalMin(value = "0.0", message = "validation.survey.progress.invalid-value.below-min")
    @DecimalMax(value = "100.0", message = "validation.survey.progress.invalid-value.above-max")
    private Float progress;

    public static SurveyProcessingStatus getSurveyProcessingStatus(String status) {
        return List.of("started", "download", "analysing").contains(status.toLowerCase()) ? SurveyProcessingStatus.PENDING :
            SurveyProcessingStatus.NOT_AVAILABLE;
    }

    public static SurveyAnalysisProgressStatus getSurveyAnalysisProgressStatus(String status) {
        switch (status) {
            case "started":
                return SurveyAnalysisProgressStatus.STARTED;
            case "download":
                return SurveyAnalysisProgressStatus.DOWNLOAD;
            case "analysing":
                return SurveyAnalysisProgressStatus.ANALYZING;
            default:
                return SurveyAnalysisProgressStatus.NOT_STARTED;
        }
    }
}
