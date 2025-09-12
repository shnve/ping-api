package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.SubArea;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.google.common.collect.Sets.newHashSet;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.NAME_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.TECHNICAL_IDENTIFIER;
import static fr.shom.ping.SwaggerConfig.Property.URN_DESCRIPTION;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(NON_NULL)
@Schema(name = "SubArea for the Search")
public class SubAreaSearchDto {
    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @Schema(description = NAME_DESCRIPTION, required = true)
    private String name;

    @Schema(description = URN_DESCRIPTION)
    private String urn;

    @NoHtml
    @Schema(description = TECHNICAL_IDENTIFIER)
    private String technicalIdentifier;

    private List<String> subAreasName;

    @Builder.Default
    private Set<SerieInformationTableSubAreaDto> series = new HashSet<>();

    @Builder.Default
    private Set<SubAreaInformationTableDto> parents = new HashSet<>();

    private Set<UUID> maritimeServiceIds;

    public SubAreaSearchDto(SubArea subArea) {
        id = subArea.getId();
        name = subArea.getName();
        urn = subArea.getUrn();
        technicalIdentifier = subArea.getTechnicalIdentifier();
        series = Optional.ofNullable(subArea.getSeries())
            .map(subAreas -> subAreas.stream().map(SerieInformationTableSubAreaDto::new).collect(Collectors.toSet()))
            .orElse(newHashSet());
        subAreasName = Optional.ofNullable(subArea.getSubAreas())
            .map(childList -> childList.stream()
                .map(child -> child.getTechnicalIdentifier() != null ? child.getTechnicalIdentifier() : child.getName())
                .sorted()
                .collect(toList()))
            .orElse(Collections.emptyList());
        parents = Optional.ofNullable(subArea.getParents())
            .map(subAreas -> subAreas.stream().map(SubAreaInformationTableDto::new).collect(Collectors.toSet()))
            .orElse(newHashSet());
        maritimeServiceIds = subArea.getMaritimeServiceIds();
    }
}
