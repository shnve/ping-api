package fr.shom.ping.model.tuple;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.shom.ping.model.Feature;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.locationtech.jts.geom.Geometry;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@Getter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
public class GeoregulationMapViewTuple implements Serializable {
    @Serial
    private static final long serialVersionUID = 5745801283771357065L;

    private static final ObjectMapper FEATURE_PROPERTIES_MAPPER = Jackson2ObjectMapperBuilder.json().build();

    private UUID id;

    @JsonProperty("isTemporary")
    private boolean isTemporary;

    @JsonProperty("isNew")
    private boolean isNew;

    private Integer mainNumberYear;

    private String mainNumberText;

    private String mainNumberAuthorityCode;

    private LocalDate mainNumberDate;

    private Feature feature;

    private LocalDate publicationDate;

    public GeoregulationMapViewTuple(UUID id, Integer mainNumberYear, String mainNumberText, String mainNumberAuthorityCode,
        LocalDate mainNumberDate, boolean isTemporary, boolean isNew, LocalDate publicationDate,
        JsonNode geometryProperties, Geometry geometry) {
        this.id = id;
        this.isTemporary = isTemporary;
        this.isNew = isNew;
        this.mainNumberYear = mainNumberYear;
        this.mainNumberText = mainNumberText;
        this.mainNumberAuthorityCode = mainNumberAuthorityCode;
        this.mainNumberDate = mainNumberDate;
        this.publicationDate = publicationDate;
        if (geometry != null) {
            this.feature = builderFeature(geometry, geometryProperties);
        }
    }

    private Feature builderFeature(Geometry geometry, JsonNode properties) {
        final Feature feature = Feature.builder().geometry(geometry).build();
        if (properties != null) {
            feature.setProperties(FEATURE_PROPERTIES_MAPPER.convertValue(properties, new TypeReference<>() {
            }));
        }
        return feature;
    }
}
