package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.validation.NoHtml;
import fr.shom.ping.validation.ValidUrl;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Abstract warning link Dto Model")
public class AbstractWarningLinkDto {

    @NotEmpty(message = "validation.warning-link.title.not-empty")
    @Schema(description = "warning link title", required = true)
    @NoHtml
    protected String title;

    @Schema(description = "warning link international title")
    @NoHtml
    protected String internationalTitle;

    @NotNull(message = "validation.warning-link.warning-id.not-null")
    @Schema(description = "warnning id", required = true)
    protected UUID warningId;

    @Schema(description = "maritime entity reference url")
    @ValidUrl
    protected String url;
}

