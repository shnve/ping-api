package fr.shom.ping.model.predicate;

import com.fasterxml.jackson.annotation.JsonFormat;
import fr.shom.ping.model.enums.GeoregulationActivity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Georegulation map search criteria")
public class GeoregulationMapSearchCriteria {
    private Boolean permanent;

    private Boolean temporary;

    private Boolean allTerritorialSea;

    private Boolean notAllTerritorialSea;

    private Boolean seasonalPeriod;

    private Boolean notSeasonalPeriod;

    @Schema(description = "Concerned activities")
    private Set<GeoregulationActivity> concernedActivities;

    @DateTimeFormat(iso = DATE)
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC")
    @Schema(description = "From date", example = "2021-01-21")
    private LocalDate dateStart;

    @DateTimeFormat(iso = DATE)
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC")
    @Schema(description = "To date", example = "2021-01-21")
    private LocalDate dateEnd;

    @DateTimeFormat(iso = DATE)
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC")
    @Schema(description = "Published from including date", example = "2021-01-21")
    private LocalDate publishedFromIncludingDate;
}
