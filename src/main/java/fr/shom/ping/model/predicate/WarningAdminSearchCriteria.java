package fr.shom.ping.model.predicate;

import com.fasterxml.jackson.annotation.JsonFormat;
import fr.shom.ping.model.enums.WarningHazardTypeDetails;
import fr.shom.ping.model.enums.WarningHazardTypeGeneral;
import fr.shom.ping.model.enums.WarningStatus;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Warning admin search criteria")
public class WarningAdminSearchCriteria {

    @Schema(description = "Serie ID", example = "b72eb02d-61cc-4577-90c4-cfd83432bf46", required = true)
    @NotNull
    private UUID serieId;

    @Schema(description = "Status", required = true)
    @NotNull
    private WarningStatus status;

    @Schema(description = "To be done (state != active)")
    private Boolean toBeDone;

    @Schema(description = "Warning number", example = "AVURNAV Brest 1001/23")
    @NoHtml
    private String warningNumber;

    @Schema(description = "AN model name", example = "Modèle 2")
    private String modelName;

    @Schema(description = "Text in locality or information", example = "001/21")
    private String localityOrInformation;

    @Schema(description = "Sub area IDs", example = "0457585d-8171-4d46-aae2-240590a6bf8a")
    private Set<UUID> subAreaIds;

    @Schema(description = "Warning hazard type general")
    private WarningHazardTypeGeneral warningHazardTypeGeneral;

    @Default
    @Schema(description = "Warning hazard type details")
    private Set<WarningHazardTypeDetails> warningHazardTypeDetails = new HashSet<>();

    private String referenceNumber;

    @DateTimeFormat(iso = DATE)
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC")
    @Schema(description = "From date", example = "2021-01-21")
    private LocalDate dateStart;

    @DateTimeFormat(iso = DATE)
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC")
    @Schema(description = "To date", example = "2021-01-21")
    private LocalDate dateEnd;

    @Schema(description = "Concerned maritime entity IDs")
    private Set<UUID> concernedMaritimeEntityIds;

    @Schema(description = "Folder IDs")
    private Set<UUID> folderIds;

    @Schema(description = "Maritime entity cover IDs")
    private Set<UUID> entityCoverIds;
}
