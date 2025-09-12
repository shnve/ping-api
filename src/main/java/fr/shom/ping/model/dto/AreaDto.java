package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Area;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Area Dto Model")
public class AreaDto extends AbstractAreaDto {

    @Schema(description = ID_DESCRIPTION)
    private UUID id;

    public AreaDto(Area area) {
        this.id = area.getId();
        this.country = area.getCountry();
        this.localizedInformation  = area.getLocalizedInformation();
        this.geom = area.getGeom();
        this.information = area.getInformation();
        this.internationalName = area.getInternationalName();
        this.disclaimer = area.getDisclaimer();
        this.internationalDisclaimer = area.getInternationalDisclaimer();
        this.name = area.getName();
        this.urn = area.getUrn();
        this.borderAreas = Optional.ofNullable(area.getBorderAreas())
            .map(areas -> areas.stream().map(Area::getId).collect(Collectors.toList()))
            .orElse(null);
    }
}
