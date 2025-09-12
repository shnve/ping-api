package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.FixedDateRange;
import fr.shom.ping.model.InfosourcePart;
import fr.shom.ping.model.enums.InfosourcePartObjectKeys;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.GEOMETRY_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.GEOMETRY_EXAMPLE;
import static fr.shom.ping.SwaggerConfig.Property.LOCALIZED_TEXT_EXAMPLE;
import static fr.shom.ping.model.enums.InfosourcePartObjectKeys.DATE_RANGES;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
@Builder
@Schema(name = "Infosource Activity Forecast Part create DTO")
public class InfosourceActivityForecastPartCreateDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 3334306037683575177L;

    @Schema(description = GEOMETRY_DESCRIPTION, example = GEOMETRY_EXAMPLE)
    @Default
    @NotEmpty(message = "validation.infosource.activity-forecast-part.localisation.not-empty")
    private Set<@Valid FeatureDto> localisation = new HashSet<>();

    @Default
    private Integer position = 1;

    @Schema(description = "List of fixed date range for activities periods. Sorted")
    @Default
    private List<@Valid FixedDateRange> dateRanges = new ArrayList<>();

    @Schema(description = "Description fr")
    @NotEmpty(message = "validation.infosource.activity-forecast-part.description-fr.not-empty")
    @NotBlank(message = "validation.infosource.activity-forecast-part.description-fr.not-blank")
    @NoHtml
    private String description;

    @NoHtml
    private String internationalDescription;

    @Schema(description = "Information", example = LOCALIZED_TEXT_EXAMPLE)
    @Default
    @NoHtml
    private Map<String, String> information = new HashMap<>();

    public static InfosourceActivityForecastPartCreateDto fromInfosourcePart(InfosourcePart infosourcePart) {
        List<Map<String, String>> objectDateRanges = ((List<Map<String, String>>) infosourcePart.getObject().get(DATE_RANGES.getKeyName()));

        return InfosourceActivityForecastPartCreateDto.builder()
            .description(infosourcePart.getDescription())
            .position(infosourcePart.getPosition())
            .information((LinkedHashMap<String, String>) infosourcePart.getObject().get(InfosourcePartObjectKeys.INFORMATION.getKeyName()))
            .internationalDescription(infosourcePart.getInternationalDescription())
            .dateRanges(new ArrayList<>(InfosourceObjectDateRangeDto.getDateRanges(objectDateRanges, null)))
            .localisation(infosourcePart.getLocalisation().stream().map(FeatureDto::new).collect(Collectors.toSet()))
            .build();
    }
}
