package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.validation.NoHtml;
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
@Schema(name = "Tag Dto Model")
public abstract class AbstractTagDto {

    @NotEmpty(message = "validation.tag.value.not-empty")
    @Schema(description = "tag value", required = true)
    @NoHtml
    protected String value;

    @NotNull(message = "validation.tag.maritime-service-id.not-null")
    @Schema(description = "maritime service id", required = true)
    protected UUID maritimeServiceId;
}
