package fr.shom.ping.model.dto;

import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Survey Result Success Dto Model")
public class SurveyResultSuccessDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1683679959574862208L;

    @NotNull(message = "validation.survey-result.success.not-null")
    private Boolean success;

    @NotNull(message = "validation.survey-result.id.not-null")
    @NoHtml
    private String id;

    @NoHtml
    private String dlLink;

    @Builder.Default
    private Set<String> messages = new HashSet<>();
}
