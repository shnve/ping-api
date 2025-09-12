package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Georegulation;
import fr.shom.ping.utils.GeoregulationUtil;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import fr.shom.ping.validation.NoHtml;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.google.common.collect.Sets.newHashSet;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(NON_NULL)
public class GeoregulationSelectDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -7449070694795736036L;

    private UUID id;

    @NoHtml
    private String number;

    public GeoregulationSelectDto(Georegulation georegulation) {
        id = georegulation.getId();
        number = GeoregulationUtil.getNumber(
            georegulation.getNumber().getYear(),
            georegulation.getNumber().getText(),
            newHashSet(georegulation.getNumber().getAuthorityCodes()),
            georegulation.getNumber().getDate()
        );
    }
}
