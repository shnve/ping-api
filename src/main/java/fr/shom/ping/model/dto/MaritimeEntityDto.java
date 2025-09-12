package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Area;
import fr.shom.ping.model.MaritimeEntity;
import fr.shom.ping.model.MaritimeEntityReference;
import fr.shom.ping.model.MaritimeService;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Maritime Entity Dto Model")
public class MaritimeEntityDto extends AbstractMaritimeEntityDto {

    @Schema(description = ID_DESCRIPTION)
    private UUID id;

    public MaritimeEntityDto(MaritimeEntity entity) {
        this.id = entity.getId();
        this.localizedInformation = entity.getLocalizedInformation();
        this.geom = entity.getGeom();
        this.information = entity.getInformation();
        this.internationalName = entity.getInternationalName();
        this.name = entity.getName();
        this.urn = entity.getUrn();
        this.type = entity.getType();
        this.maritimeServiceId = entity.getMaritimeService() != null ? entity.getMaritimeService().getId() : null;
        this.areas = Optional.ofNullable(entity.getAreas())
            .map(areas -> areas.stream().map(Area::getId).collect(Collectors.toList()))
            .orElse(null);
        this.sissyId = entity.getSissyId();
        if (isNotEmpty(entity.getAdditionalServices())) {
            this.additionalServices = entity.getAdditionalServices().stream().map(MaritimeService::getId).collect(Collectors.toSet());
        };
        if (isNotEmpty(entity.getReferences())) {
            this.references = entity.getReferences().stream().map(MaritimeEntityReference::asDto).collect(Collectors.toSet());
        }
    }

    public static MaritimeEntityDto fromMaritimeEntity(MaritimeEntity entity) {
        return MaritimeEntityDto.builder()
            .id(entity.getId())
            .localizedInformation(entity.getLocalizedInformation())
            .geom(entity.getGeom())
            .information(entity.getInformation())
            .internationalName(entity.getInternationalName())
            .name(entity.getName())
            .urn(entity.getUrn())
            .type(entity.getType())
            .maritimeServiceId(entity.getMaritimeService() != null ? entity.getMaritimeService().getId() : null)
            .areas(Optional.ofNullable(entity.getAreas())
                .map(areas -> areas.stream().map(Area::getId).collect(Collectors.toList()))
                .orElse(null))
            .additionalServices(Optional.ofNullable(entity.getAdditionalServices())
                .map(services -> services.stream().map(MaritimeService::getId).collect(Collectors.toSet()))
                .orElse(null))
            .build();
    }
}
