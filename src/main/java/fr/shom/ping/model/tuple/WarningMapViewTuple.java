package fr.shom.ping.model.tuple;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.shom.ping.model.Feature;
import fr.shom.ping.model.enums.SerieType;
import fr.shom.ping.model.enums.WarningHazardTypeDetails;
import fr.shom.ping.model.enums.WarningHazardTypeGeneral;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.locationtech.jts.geom.Geometry;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import static org.springframework.util.StringUtils.hasText;

@Getter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
public class WarningMapViewTuple implements Serializable {
    @Serial
    private static final long serialVersionUID = -1309810570681135243L;
    private static final ObjectMapper FEATURE_PROPERTIES_MAPPER = Jackson2ObjectMapperBuilder.json().build();

    private UUID id;
    private String nameOfSeries;
    private Integer warningNumber;
    private Integer year;
    private WarningHazardTypeGeneral warningHazardTypeGeneral;
    private WarningHazardTypeDetails warningHazardTypeDetails;
    private SerieType serieType;
    private Instant publicationTime;
    private Feature geometry;
    private Feature affectedArea;

    public WarningMapViewTuple(UUID id, String nameOfSeries, Integer warningNumber, Integer year,
                               String warningHazardTypeGeneral, String warningHazardTypeDetails, String serieType, Instant publicationTime,
                               JsonNode geometryProperties, Geometry geometry,
                               JsonNode affectedAreaProperties, Geometry affectedArea) {
        this.id = id;
        this.nameOfSeries = nameOfSeries;
        this.warningNumber = warningNumber;
        this.year = year;
        this.warningHazardTypeGeneral = WarningHazardTypeGeneral.valueOf(warningHazardTypeGeneral);
        if (hasText(warningHazardTypeDetails)) {
            this.warningHazardTypeDetails = WarningHazardTypeDetails.valueOf(warningHazardTypeDetails);
        }
        this.serieType = SerieType.valueOf(serieType);
        this.publicationTime = publicationTime;
        if (geometry != null) {
            this.geometry = builderFeature(geometry, geometryProperties);
        }
        if (affectedArea != null) {
            this.affectedArea = builderFeature(affectedArea, affectedAreaProperties);
        }
    }

    private Feature builderFeature(Geometry geometry, JsonNode properties) {
        Feature feature = new Feature();
        feature.setGeometry(geometry);
        if (properties != null) {
            feature.setProperties(FEATURE_PROPERTIES_MAPPER.convertValue(properties, new TypeReference<>() {}));
        }

        return feature;
    }
}
