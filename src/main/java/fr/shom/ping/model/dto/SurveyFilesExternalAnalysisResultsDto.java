package fr.shom.ping.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Survey Files Analysis Results Dto Model")
public class SurveyFilesExternalAnalysisResultsDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 8733856027128033045L;

    protected String filename;

    protected Object errors;
}
