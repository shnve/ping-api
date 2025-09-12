package fr.shom.ping.model.dto;

import fr.shom.ping.model.enums.GeoregulationPartType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoregulationPartUpdateDto implements Serializable, Comparable<GeoregulationPartUpdateDto> {
    @Default
    private List<GeoregulationPartType> types = new ArrayList<>();

    @Transient
    private Integer position;

    @Override
    public int compareTo(GeoregulationPartUpdateDto part) {
        return position.compareTo(part.getPosition());
    }
}
