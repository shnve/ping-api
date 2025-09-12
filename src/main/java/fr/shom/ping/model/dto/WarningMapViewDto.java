package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Feature;
import fr.shom.ping.model.enums.GlobalWarningType;
import fr.shom.ping.model.enums.SerieType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.GEOMETRY_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.GEOMETRY_EXAMPLE;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(NON_NULL)
@Schema(name = "Warning Map View Dto Model")
public class WarningMapViewDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -4032098244622611195L;

    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @Schema(description = "Name of series", required = true)
    private String nameOfSeries;

    @Schema(description = "Warning number", required = true)
    private Integer warningNumber;

    @Schema(description = "Year", required = true)
    private Integer year;

    @Schema(description = "Global warning type", required = true)
    private GlobalWarningType globalWarningType;

    @Schema(description = "Warning details", required = true)
    private String warningTypeDetails;

    @JsonIgnore
    private SerieType serieType;

    @Schema(description = "Publication time", required = true)
    private Instant publicationTime;

    @Schema(description = GEOMETRY_DESCRIPTION, example = GEOMETRY_EXAMPLE)
    @Default
    private Set<Feature> geometries = new HashSet<>();

    @Schema(description = GEOMETRY_DESCRIPTION, example = GEOMETRY_EXAMPLE)
    @Default
    private Set<Feature> affectedAreas = new HashSet<>();

    @Schema(description = "Priority", required = true)
    public Integer getPriority() {
        return serieType.getCode();
    }
}
