package fr.shom.ping.model.tuple;

import fr.shom.ping.model.Feature;
import fr.shom.ping.model.enums.SerieType;
import fr.shom.ping.model.enums.WarningHazardTypeDetails;
import fr.shom.ping.model.enums.WarningHazardTypeGeneral;
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

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarningMapViewAggregatedTuple implements Serializable {
    @Serial
    private static final long serialVersionUID = 2713074232030220054L;

    private UUID id;
    private String nameOfSeries;
    private Integer warningNumber;
    private Integer year;
    private WarningHazardTypeGeneral warningHazardTypeGeneral;
    private SerieType serieType;
    private Instant publicationTime;
    @Default
    private Set<WarningHazardTypeDetails> warningHazardTypeDetails = new HashSet<>();
    @Default
    private Set<Feature> geometries = new HashSet<>();
    @Default
    private Set<Feature> affectedAreas = new HashSet<>();
}
