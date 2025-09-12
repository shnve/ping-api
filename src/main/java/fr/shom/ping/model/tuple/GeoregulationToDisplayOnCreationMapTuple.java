package fr.shom.ping.model.tuple;

import fr.shom.ping.model.Feature;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoregulationToDisplayOnCreationMapTuple {
    private UUID id;
    private Feature geometry;
}
