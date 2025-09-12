package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@JsonInclude(NON_NULL)
@Schema(name = "Georegulation update dto model")
public class GeoregulationUpdateDto extends GeoregulationCreateDto {
    @Serial
    private static final long serialVersionUID = 423115125656862757L;

    @NotNull(message = "validation.georegulation.id.not-null")
    @Schema(description = ID_DESCRIPTION, required = true)
    protected UUID id;
}
