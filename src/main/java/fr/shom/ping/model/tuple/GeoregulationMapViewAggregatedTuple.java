package fr.shom.ping.model.tuple;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.shom.ping.model.Feature;
import fr.shom.ping.model.dto.GeoregulationAdminTableDto;
import fr.shom.ping.utils.GeoregulationUtil;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
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
public class GeoregulationMapViewAggregatedTuple implements Serializable {
    @Serial
    private static final long serialVersionUID = 7233589686253978704L;

    private UUID id;

    @JsonProperty("isTemporary")
    private boolean isTemporary;

    @JsonProperty("isNew")
    private boolean isNew;

    private LocalDate publicationDate;

    @JsonIgnore
    private GeoregulationAdminTableDto.GeoregulationAdminTableNumberDto mainNumber;

    public String getNumber() {
        return GeoregulationUtil.getNumber(mainNumber.getYear(), mainNumber.getText(), mainNumber.getAuthorityCodes(), mainNumber.getDate());
    }

    @Default
    private Set<Feature> features = new HashSet<>();
}
