package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.SubArea;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.google.common.collect.Sets.newHashSet;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Sub Area Dto Model")
public class SubAreaDetailsDto extends AbstractSubAreaDto {

    @Schema(description = ID_DESCRIPTION)
    private UUID id;

    public SubAreaDetailsDto(SubArea area) {
        this.id = area.getId();
        this.geom = area.getGeom();
        this.name = area.getName();
        this.internationalName = area.getInternationalName();
        this.urn = area.getUrn();
        this.technicalIdentifier = area.getTechnicalIdentifier();
        this.subAreas = Optional.ofNullable(area.getSubAreas())
            .map(subAreas -> subAreas.stream().map(SubArea::getId).collect(Collectors.toSet()))
            .orElse(newHashSet());
        this.parents = Optional.ofNullable(area.getParents())
            .map(subAreas -> subAreas.stream().map(SubArea::getId).collect(Collectors.toSet()))
            .orElse(newHashSet());
        if (area.getArea() != null) {
            this.areaId = area.getArea().getId();
        }
    }
}
