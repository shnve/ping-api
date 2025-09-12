package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.shom.ping.model.Feature;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(NON_NULL)
@Schema(name = "Georegulation Map View Dto Model")
public class GeoregulationMapViewDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 3511279119406107440L;

    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @JsonProperty("isTemporary")
    private boolean isTemporary;

    @JsonProperty("isNew")
    private boolean isNew;

    @Schema(description = "Formatted main number with authority code")
    @NoHtml
    private String mainNumber;

    @Schema(description = "Features")
    @Default
    private Set<Feature> features = new HashSet<>();

    private LocalDate publicationDate;
}
