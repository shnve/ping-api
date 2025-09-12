package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.validation.NoHtml;
import fr.shom.ping.validation.ValidCountryCode;
import fr.shom.ping.validation.ValidURN;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.locationtech.jts.geom.Geometry;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.COUNTRY_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.DISCLAIMER;
import static fr.shom.ping.SwaggerConfig.Property.EMPTY_LIST_EXAMPLE;
import static fr.shom.ping.SwaggerConfig.Property.GEOMETRY_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.GEOMETRY_EXAMPLE;
import static fr.shom.ping.SwaggerConfig.Property.INFORMATION_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.INTERNATIONAL_DISCLAIMER;
import static fr.shom.ping.SwaggerConfig.Property.INTERNATIONAL_NAME_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.LOCALIZED_INFORMATION_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.NAME_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.URN_DESCRIPTION;

@Data
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Area Dto Model")
public abstract class AbstractAreaDto {

    @ValidURN(message = "validation.area.urn.invalid")
    @Schema(description = URN_DESCRIPTION)
    protected String urn;

    @NotEmpty(message = "validation.area.name.not-empty")
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
    protected String localizedInformation ;

    @Schema(description = DISCLAIMER)
    @NoHtml
    protected String disclaimer;

    @Schema(description = INTERNATIONAL_DISCLAIMER)
    @NoHtml
    protected String internationalDisclaimer;

    @ValidCountryCode(message = "validation.area.country-code.invalid")
    @Schema(description = COUNTRY_DESCRIPTION)
    @NoHtml
    protected String country;

    @NotNull(message = "validation.area.geometry.not-null")
    @Schema(description = GEOMETRY_DESCRIPTION, required = true, example = GEOMETRY_EXAMPLE)
    protected Geometry geom;

    @Schema(description = "List of border areas", example = EMPTY_LIST_EXAMPLE)
    protected List<UUID> borderAreas;

}
