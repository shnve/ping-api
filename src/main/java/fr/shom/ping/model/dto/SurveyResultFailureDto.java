package fr.shom.ping.model.dto;

import fr.shom.ping.model.enums.SurveyResultFailureMessage;
import fr.shom.ping.model.enums.UserTitle;
import fr.shom.ping.validation.ValueOfEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Survey Result Failure Dto Model")
public class SurveyResultFailureDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -3256945469989947417L;

    @NotBlank(message = "validation.survey.failure-result.not-blank")
    @ValueOfEnum(enumClass = SurveyResultFailureMessage.class, message = "validation.survey.failure-result.message.not-in-enum")
    private String message;
}
