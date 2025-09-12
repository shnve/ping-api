package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.Serie;
import fr.shom.ping.model.SubArea;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.google.common.collect.ImmutableSortedSet.toImmutableSortedSet;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toSet;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Serie Details Dto Model")
public class SerieDetailsDto extends AbstractSerieDto {
    @Serial
    private static final long serialVersionUID = 7355511947136237754L;

    @Schema(description = ID_DESCRIPTION)
    private UUID id;

    public SerieDetailsDto(Serie serie) {
        id = serie.getId();
        urn = serie.getUrn();
        name = serie.getName();
        internationalName = serie.getInternationalName();
        information = serie.getInformation();
        localizedInformation = serie.getLocalizedInformation();
        type = serie.getType();
        mandatoryLanguages = serie.getMandatoryLanguages();
        optionalLanguages = serie.getOptionalLanguages();
        description = serie.getDescription();
        maritimeService = Optional.ofNullable(serie.getMaritimeService()).map(MaritimeService::getId).orElse(null);
        subAreas = Optional.ofNullable(serie.getSubAreas())
            .map(subAreas -> subAreas.stream().map(SubArea::getId).collect(toSet()))
            .orElse(new HashSet<>());
        ranges = serie.getRanges().entrySet().stream()
            .map(entry -> new Integer[]{entry.getKey(), entry.getValue()})
            .collect(toImmutableSortedSet(comparing(range -> range[0])));
        navtexSource = serie.getNavtexSource();
        s124Identifier = serie.getS124Identifier();
    }
}
