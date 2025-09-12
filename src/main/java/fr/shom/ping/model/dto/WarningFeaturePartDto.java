package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Feature;
import fr.shom.ping.model.FixedDateRange;
import fr.shom.ping.model.LocalizedText;
import fr.shom.ping.model.enums.WarningHazardTypeDetails;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.GEOMETRY_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.GEOMETRY_EXAMPLE;
import static fr.shom.ping.SwaggerConfig.Property.LOCALIZED_TEXT_EXAMPLE;
import static java.util.stream.Collectors.toMap;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
@Builder
@Schema(name = "Warning Feature Part DTO")
public class WarningFeaturePartDto implements Serializable, Comparable<WarningFeaturePartDto> {
    @Serial
    private static final long serialVersionUID = 9036430590562634368L;

    @JsonIgnore
    @Default
    private Integer position = 1;

    @Schema(description = GEOMETRY_DESCRIPTION, example = GEOMETRY_EXAMPLE)
    @Default
    private Set<Feature> geometries = new HashSet<>();

    @Schema(description = GEOMETRY_DESCRIPTION, example = GEOMETRY_EXAMPLE)
    @Default
    private Set<Feature> affectedAreas = new HashSet<>();

    @Schema(description = "Warning hazard type details")
    @Default
    private Set<WarningHazardTypeDetails> typeDetails = new HashSet<>();

    @Schema(description = "Information", example = LOCALIZED_TEXT_EXAMPLE)
    @Default
    private Set<LocalizedText> information = new HashSet<>();

    @Schema(description = "List of fixed date range. Sorted")
    @Default
    private SortedSet<FixedDateRange> dateRanges = new TreeSet<>();

    @Override
    public int compareTo(WarningFeaturePartDto featurePart) {
        return position.compareTo(featurePart.getPosition());
    }

    public Map<String, String> getInformation() {
        return information.stream()
            .collect(toMap(LocalizedText::getLanguage, LocalizedText::getText));
    }

    public void addInformation(LocalizedText localizedText) {
        this.information.add(localizedText);
    }
}
