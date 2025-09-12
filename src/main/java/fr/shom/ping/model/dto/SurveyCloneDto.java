package fr.shom.ping.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Survey clone Dto Model")
public class SurveyCloneDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -1926965739079539044L;

    @NotEmpty(message = "validation.survey.name-and-location.not-empty")
    @NotBlank(message = "validation.survey.name-and-location.not-blank")
    protected String nameAndLocation;

    @NotNull(message = "validation.survey.area-id.not-null")
    protected UUID areaId;
}
