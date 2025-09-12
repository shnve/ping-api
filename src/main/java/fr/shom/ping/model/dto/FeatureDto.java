package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Feature;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Geometry;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
@Builder
@Schema(name = "Feature DTO")
public class FeatureDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -2662841588379711590L;

    @Default
    @NoHtml
    private Map<String, Object> properties = new HashMap<>();

    @NotNull(message = "validation.feature.geometry.not-null")
    private Geometry geometry;

    public FeatureDto(Feature feature) {
        properties = feature.getProperties();
        geometry = feature.getGeometry();
    }

    public Feature toFeature() {
        return Feature.builder()
            .properties(properties)
            .geometry(geometry)
            .build();
    }
}
