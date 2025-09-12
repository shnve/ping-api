package fr.shom.ping.model.predicate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.enums.ActionType;
import fr.shom.ping.model.enums.InfosourceNature;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.format.annotation.DateTimeFormat;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Infosource admin search criteria")
public class InfosourceAdminSearchCriteria extends AbstractInfosourceSearchCriteria {

    @Schema(description = "Maritime service ID", required = true)
    @NotNull
    private UUID issuingServiceId;

    @Schema(description = "Display rejected")
    private boolean rejected;

    @Schema(description = "Display archived")
    private boolean archived;

    @Default
    @Schema(description = "nature")
    private Set<InfosourceNature> natures = new HashSet<>();

    @Default
    @Schema(description = "concerned maritime entity ids")
    private Set<UUID> maritimeEntityIds = new HashSet<>();

    @Default
    @Schema(description = "tags")
    private Set<String> tags = new HashSet<>();

    @DateTimeFormat(iso = DATE)
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC")
    @Schema(description = "Last update from date", example = "2021-01-21")
    private LocalDate lastUpdatePeriodDateStart;

    @DateTimeFormat(iso = DATE)
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC")
    @Schema(description = "Last update to date", example = "2021-01-21")
    private LocalDate lastUpdatePeriodDateEnd;

    @Schema(description = "Infosource number")
    private String number;

    @DateTimeFormat(iso = DATE)
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC")
    @Schema(description = "Sending from date", example = "2021-01-21")
    private LocalDate sendingPeriodDateStart;

    @DateTimeFormat(iso = DATE)
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC")
    @Schema(description = "Sending to date", example = "2021-01-21")
    private LocalDate sendingPeriodDateEnd;

    @Schema(description = "Sending year")
    private String sendingYear;

    @Default
    @Schema(description = "maritime services as recipient ids", example = "0457585d-8171-4d46-aae2-240590a6bf8a")
    private Set<UUID> recipientIds = new HashSet<>();

    @Schema(description = "Affair number")
    private String affairNumber;

    @Default
    @Schema(description = "Service statuses")
    private Set<ActionType> serviceStatuses = new HashSet<>();
}
