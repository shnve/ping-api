package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Area;
import fr.shom.ping.model.MaritimeEntity;
import fr.shom.ping.model.enums.MaritimeEntityType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.NAME_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.URN_DESCRIPTION;

@Data
@Builder
@JsonInclude(NON_NULL)
@AllArgsConstructor
@RequiredArgsConstructor
@Schema(name = "Maritime Entity Search Dto Model")
public class MaritimeEntitySearchResultDto {

    @Schema(description = ID_DESCRIPTION)
    private UUID id;

    @Schema(description = URN_DESCRIPTION)
    private String urn;

    @Schema(description = NAME_DESCRIPTION)
    private String name;

    @Schema(description = "Maritime Entity Type")
    private MaritimeEntityType type;

    @Schema(description = "List of linked areas")
    private List<Area> areas;

    private MaritimeServiceSharedDto maritimeService;

    private Set<MaritimeServiceSharedDto> additionalServices;

    public static MaritimeEntitySearchResultDto fromMaritimeEntity(MaritimeEntity entity) {
        return MaritimeEntitySearchResultDto.builder()
            .id(entity.getId())
            .name(entity.getName())
            .urn(entity.getUrn())
            .type(entity.getType())
            .maritimeService(entity.getMaritimeService() != null ? MaritimeServiceSharedDto.fromMaritimeService(entity.getMaritimeService()) : null)
            .areas(entity.getAreas())
            .additionalServices(Optional.ofNullable(entity.getAdditionalServices())
                .map(services -> services.stream().map(MaritimeServiceSharedDto::fromMaritimeService).collect(Collectors.toSet()))
                .orElse(null))
            .build();
    }
}
