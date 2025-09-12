package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Georegulation;
import fr.shom.ping.utils.GeoregulationUtil;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(NON_NULL)
public class GeoregulationReferenceDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -3152823950346978810L;

    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @NoHtml
    private String number;

    private Instant creationDate;

    private Instant publicationDate;

    public static GeoregulationReferenceDto fromGeoregulation(Georegulation georegulation) {
        return GeoregulationReferenceDto.builder()
            .id(georegulation.getId())
            .number(GeoregulationUtil.getNumber(
                georegulation.getNumber().getYear(),
                georegulation.getNumber().getText(),
                Set.copyOf(georegulation.getNumber().getAuthorityCodes()),
                georegulation.getNumber().getDate()
            ))
            .creationDate(georegulation.getCreationDate())
            .publicationDate(georegulation.getPublicationDate())
            .build();
    }
}
