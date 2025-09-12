package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.enums.MaritimeEntityType;
import fr.shom.ping.validation.NoHtml;
import fr.shom.ping.validation.ValidURN;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.locationtech.jts.geom.Geometry;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.EMPTY_LIST_EXAMPLE;
import static fr.shom.ping.SwaggerConfig.Property.GEOMETRY_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.GEOMETRY_EXAMPLE;
import static fr.shom.ping.SwaggerConfig.Property.INFORMATION_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.INTERNATIONAL_NAME_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.LOCALIZED_INFORMATION_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.NAME_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.URN_DESCRIPTION;

@Data
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Maritime Entity Dto Model")
public abstract class AbstractMaritimeEntityDto {

    @ValidURN(message = "validation.maritime-entity.urn.invalid")
    @Schema(description = URN_DESCRIPTION)
    protected String urn;

    @NotEmpty(message = "validation.maritime-entity.name.not-empty")
    @Schema(description = NAME_DESCRIPTION, required = true)
    @NoHtml
    protected String name;

    @Schema(description = INTERNATIONAL_NAME_DESCRIPTION)
    @NoHtml
    protected String internationalName;

    @Schema(description = INFORMATION_DESCRIPTION)
    @NoHtml
    protected String information;

    @Schema(description = LOCALIZED_INFORMATION_DESCRIPTION)
    @NoHtml
    protected String localizedInformation;

    @NotNull(message = "validation.maritime-entity.type.not-null")
    @Schema(description = "Maritime Entity Type", required = true)
    protected MaritimeEntityType type;

    @Schema(description = "Maritime Entity Service Id")
    protected UUID maritimeServiceId;

    @NotNull(message = "validation.maritime-entity.geometry.not-null")
    @Schema(description = GEOMETRY_DESCRIPTION, required = true, example = GEOMETRY_EXAMPLE)
    protected Geometry geom;

    @Schema(description = "List of linked areas", example = EMPTY_LIST_EXAMPLE)
    protected List<UUID> areas;

    @Schema(description = "Sissy ID")
    @NoHtml
    protected String sissyId;

    @Schema(description = "List of references", example = EMPTY_LIST_EXAMPLE)
    @Builder.Default
    protected Set<@Valid MaritimeEntityReferenceDto> references = new HashSet<>();

    @Schema(description = "List of observer services", example = EMPTY_LIST_EXAMPLE)
    @Builder.Default
    protected Set<UUID> additionalServices = new HashSet<>();
}
