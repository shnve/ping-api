package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Feature;
import java.util.List;
import java.util.UUID;
import fr.shom.ping.model.enums.GeoregulationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(NON_NULL)
public class GeoregulationToDisplayOnCreationMapDto {
    private UUID id;
    private List<Feature> features;
    private GeoregulationType type;
}
