package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.SurveyDetection;
import java.io.Serial;
import java.io.Serializable;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Geometry;
import org.modelmapper.ModelMapper;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(NON_NULL)
public class SurveyDetectionDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -3895546796030243735L;

    public static final ModelMapper MODEL_MAPPER = new ModelMapper();

    @NotNull(message = "validation.survey-detection.geometry.not-null")
    private Geometry geometry;

    @NotNull(message = "validation.survey-detection.depth.not-null")
    private Float depth;

    @NotNull(message = "validation.survey-detection.difference.not-null")
    private Float difference;

    @NotNull(message = "validation.survey-detection.color.not-null")
    private Short color;


    public static Set<SurveyDetectionDto> mapToSurveyDetectionDto(Set<SurveyDetection> surveyDetections) {
        return surveyDetections.stream()
            .map(detection -> MODEL_MAPPER.map(detection, SurveyDetectionDto.class))
            .collect(Collectors.toSet());
    }
}
