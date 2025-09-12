package fr.shom.ping.model.predicate;

import com.fasterxml.jackson.annotation.JsonFormat;
import fr.shom.ping.model.enums.GeoregulationActivity;
import fr.shom.ping.model.enums.GeoregulationStatus;
import fr.shom.ping.model.enums.GeoregulationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
@Schema(name = "Georegulation admin search criteria")
public class GeoregulationAdminSearchCriteria {

    @NotNull(message = "validation.georegulation-admin-search-criteria.status.not-null")
    private GeoregulationStatus status;

    @NotNull(message = "validation.georegulation-admin-search-criteria.issuing-service-id.not-null")
    private UUID issuingServiceId;

    private Integer mainNumberYear;

    private String mainNumberText;

    @Default
    private List<UUID> mainNumberAuthorityIds = new ArrayList<>();

    @DateTimeFormat(iso = DATE)
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC")
    @Schema(description = "Main number date", example = "2021-01-21")
    private LocalDate mainNumberDate;

    private GeoregulationType type;

    @Default
    private List<UUID> maritimeEntityIds = new ArrayList<>();

    @Default
    private List<GeoregulationActivity> activities = new ArrayList<>();

    private Boolean isPublic;

    private Boolean isNew;

    private Boolean isExpired;
}
