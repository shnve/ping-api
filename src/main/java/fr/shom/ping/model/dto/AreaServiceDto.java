package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Area;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.INTERNATIONAL_NAME_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.NAME_DESCRIPTION;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
@Builder
@Schema(name = "Area service DTO")
public class AreaServiceDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -7257897118919377248L;

    @Schema(description = ID_DESCRIPTION)
    private UUID id;

    @NotEmpty(message = "validation.area.name.not-empty")
    @Schema(description = NAME_DESCRIPTION, required = true)
    protected String name;

    @Schema(description = INTERNATIONAL_NAME_DESCRIPTION)
    protected String internationalName;

    public static AreaServiceDto fromArea(Area area) {
        return AreaServiceDto.builder()
            .id(area.getId())
            .name(area.getName())
            .internationalName(area.getInternationalName())
            .build();
    }
}
