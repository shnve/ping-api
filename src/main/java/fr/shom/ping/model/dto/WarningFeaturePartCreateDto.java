package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.FixedDateRange;
import fr.shom.ping.model.enums.WarningHazardTypeDetails;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.GEOMETRY_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.GEOMETRY_EXAMPLE;
import static fr.shom.ping.SwaggerConfig.Property.LOCALIZED_TEXT_EXAMPLE;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
@Builder
@Schema(name = "Warning Feature Create Part DTO")
public class WarningFeaturePartCreateDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 5348153431308419123L;

    @Schema(description = GEOMETRY_DESCRIPTION, example = GEOMETRY_EXAMPLE)
    @Default
    private Set<FeatureDto> geometries = new HashSet<>();

    @Schema(description = GEOMETRY_DESCRIPTION, example = GEOMETRY_EXAMPLE)
    @Default
    private Set<FeatureDto> affectedAreas = new HashSet<>();

    @Schema(description = "Warning hazard type details")
    @Default
    private Set<WarningHazardTypeDetails> typeDetails = new HashSet<>();

    @Schema(description = "Information", example = LOCALIZED_TEXT_EXAMPLE)
    @Default
    @NoHtml
    private Map<String, String> information = new HashMap<>();

    @Schema(description = "List of fixed date range. Sorted")
    @Default
    private List<@Valid FixedDateRange> dateRanges = new ArrayList<>();
}
