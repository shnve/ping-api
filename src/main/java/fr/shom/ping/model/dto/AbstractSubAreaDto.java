package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.validation.NoHtml;
import fr.shom.ping.validation.ValidURN;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.locationtech.jts.geom.Geometry;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.GEOMETRY_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.GEOMETRY_EXAMPLE;
import static fr.shom.ping.SwaggerConfig.Property.INTERNATIONAL_NAME_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.NAME_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.TECHNICAL_IDENTIFIER;
import static fr.shom.ping.SwaggerConfig.Property.URN_DESCRIPTION;

@Data
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Sub Area Dto Model")
public abstract class AbstractSubAreaDto {

    @ValidURN(message = "validation.area.urn.invalid")
    @Schema(description = URN_DESCRIPTION)
    protected String urn;

    @NoHtml
    @Schema(description = TECHNICAL_IDENTIFIER)
    protected String technicalIdentifier;

    @NotEmpty(message = "validation.area.name.not-empty")
    @Schema(description = NAME_DESCRIPTION, required = true)
    @NoHtml
    protected String name;

    @Schema(description = INTERNATIONAL_NAME_DESCRIPTION)
    @NoHtml
    protected String internationalName;

    @Schema(description = GEOMETRY_DESCRIPTION, example = GEOMETRY_EXAMPLE)
    protected Geometry geom;

    @Default
    protected Set<UUID> subAreas = new HashSet<>();

    @Default
    protected Set<UUID> parents = new HashSet<>();

    protected UUID areaId;

}
