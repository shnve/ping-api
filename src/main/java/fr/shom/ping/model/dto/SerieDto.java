package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.enums.SerieType;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.INTERNATIONAL_NAME_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.NAME_DESCRIPTION;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(NON_NULL)
@Schema(name = "Serie Dto Model")
public class SerieDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 7991089067446403083L;

    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @Schema(description = NAME_DESCRIPTION, required = true)
    @NoHtml
    private String name;

    @Schema(description = INTERNATIONAL_NAME_DESCRIPTION)
    @NoHtml
    private String internationalName;

    @Schema(description = "Serie Type", required = true)
    private SerieType type;

    @Schema(description = "Sub area", required = true)
    @Default
    private Set<@Valid SubAreaDto> subAreas = new HashSet<>();


    @Schema(description = "Mandatory languages")
    @Default
    @NoHtml
    private Set<String> mandatoryLanguages = new HashSet<>();

    @Schema(description = "Order")
    public Integer getOrder() {
        return type != null ? type.getOrder() : null;
    }
}
