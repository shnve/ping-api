package fr.shom.ping.model.dto;

import fr.shom.ping.model.SurveyDanger;
import fr.shom.ping.model.SurveyDetection;
import fr.shom.ping.model.SurveyEnc;
import fr.shom.ping.model.enums.SurveyProcessingResultError;
import fr.shom.ping.model.enums.SurveyProcessingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import static com.google.common.collect.Sets.newHashSet;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Survey Analysis Results Dto Model")
public class SurveyExternalAnalysisResultsDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -7311382194370760813L;

    private String globalErrorMessage;

    @Builder.Default
    @EqualsAndHashCode.Exclude
    private Set<SurveyProcessingResultError> processingResultErrors = new HashSet<>();

    private SurveyProcessingStatus processingStatus;

    @Builder.Default
    @EqualsAndHashCode.Exclude
    private Set<SurveyFilesExternalAnalysisResultsDto> surveyFilesExternalAnalysisResults = new HashSet<>();

    @EqualsAndHashCode.Exclude
    @Builder.Default
    protected Set<SurveyDanger> dangers = newHashSet();

    @EqualsAndHashCode.Exclude
    @Builder.Default
    protected Set<SurveyDetection> detections = newHashSet();

    @EqualsAndHashCode.Exclude
    @Builder.Default
    private Set<SurveyEnc> enc = newHashSet();

    @EqualsAndHashCode.Exclude
    @Builder.Default
    private Set<AttachmentSurveyResultDownloadDto> resultFiles = newHashSet();

    private Instant receptionDate;

    protected SurveyAnalysisProgressDto analysisProgress;

    @EqualsAndHashCode.Exclude
    @Builder.Default
    private boolean shouldBeReported = false;  // encourage user to report only if analysis results contains detections
}
