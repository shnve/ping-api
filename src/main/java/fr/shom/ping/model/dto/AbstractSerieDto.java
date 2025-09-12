package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.enums.DiffusionNavtexSource;
import fr.shom.ping.model.enums.SerieType;
import fr.shom.ping.validation.AtMostTwoLanguages;
import fr.shom.ping.validation.NoHtml;
import fr.shom.ping.validation.ValidLanguage;
import fr.shom.ping.validation.ValidURN;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.INFORMATION_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.INTERNATIONAL_NAME_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.LOCALIZED_INFORMATION_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.NAME_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.URN_DESCRIPTION;

@Data
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Abstract Serie Dto Model")
@AtMostTwoLanguages
public abstract class AbstractSerieDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -1103047920401281258L;

    @ValidURN(message = "validation.serie.urn.invalid")
    @Schema(description = URN_DESCRIPTION)
    protected String urn;

    @NotEmpty(message = "validation.serie.name.not-empty")
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

    @NotNull(message = "validation.serie.type.not-null")
    @Schema(description = "Serie Type", required = true)
    protected SerieType type;

    @Size(min = 1, message = "validation.serie.mandatory-languages.size")
    @Schema(description = "List of mandatory languages", required = true, example = "[ \"fr\" ]")
    @EqualsAndHashCode.Exclude
    @Default
    @NoHtml
    protected Set<@ValidLanguage(message = "validation.serie.optional-languages.invalid") String> mandatoryLanguages = new HashSet<>();

    @Schema(description = "List of optional languages", example = "[ \"en\" ]")
    @EqualsAndHashCode.Exclude
    @Default
    @NoHtml
    protected Set<@ValidLanguage(message = "validation.serie.optional-languages.invalid") String> optionalLanguages = new HashSet<>();

    @Schema(description = "Description")
    @NoHtml
    protected String description;

    @NotNull(message = "validation.serie.maritime-service.not-null")
    @Schema(description = "Maritime Service", required = true)
    protected UUID maritimeService;

    @Schema(description = "ranges")
    @Default
    protected Set<Integer[]> ranges = new HashSet<>();

    @Default
    protected Set<UUID> subAreas = new HashSet<>();

    @Schema(description = "navtex source")
    protected DiffusionNavtexSource navtexSource;

    @Schema(description = "S124 identifier")
    @NoHtml
    protected String s124Identifier;
}
