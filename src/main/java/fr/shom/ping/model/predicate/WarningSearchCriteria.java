package fr.shom.ping.model.predicate;

import com.fasterxml.jackson.annotation.JsonFormat;
import fr.shom.ping.model.enums.GlobalWarningType;
import fr.shom.ping.model.enums.SerieType;
import fr.shom.ping.model.enums.WarningHazardTypeGeneral;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
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
@Schema(name = "Warning search criteria")
public class WarningSearchCriteria {

    @DateTimeFormat(iso = DATE)
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC")
    @Schema(description = "Published from including date", example = "2021-01-21")
    private LocalDate publishedFromIncludingDate;

    @Schema(description = "Global warning type")
    private Set<GlobalWarningType> globalWarningType;

    @Schema(description = "Types of serie")
    private Set<SerieType> serieType;

    @DateTimeFormat(iso = DATE)
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC")
    @Schema(description = "From date", example = "2021-01-21")
    private LocalDate dateStart;

    @DateTimeFormat(iso = DATE)
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC")
    @Schema(description = "To date", example = "2021-01-21")
    private LocalDate dateEnd;

    @Schema(description = "General warning hazard type")
    private Set<WarningHazardTypeGeneral> warningHazardTypeGenerals;

    @Schema(description = "Serie ID", example = "b72eb02d-61cc-4577-90c4-cfd83432bf46")
    private UUID serieId;

    @Schema(description = "Sub area ID", example = "0457585d-8171-4d46-aae2-240590a6bf8a")
    private Set<UUID> subAreaIds;

    @Schema(description = "Concerned maritime entity IDs")
    private Set<UUID> concernedMaritimeEntityIds;

    @Schema(description = "Maritime entity cover IDs")
    private Set<UUID> entityCoverIds;

    @Schema(description = "Reservation number and year of warning", example = "1001/23")
    @NoHtml
    private String reservationNumberAndYear;
}
