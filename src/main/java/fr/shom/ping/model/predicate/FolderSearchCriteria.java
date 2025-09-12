package fr.shom.ping.model.predicate;

import com.fasterxml.jackson.annotation.JsonFormat;
import fr.shom.ping.model.enums.FolderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Folder search criteria")
public class FolderSearchCriteria {

    @Schema(description = "Status in table (open, closed)", required = true)
    protected FolderStatus status;

    @Schema(description = "Area id")
    protected UUID areaId;

    @Schema(description = "Follower service id")
    protected UUID followerServiceId;

    @Schema(description = "Folder second part number")
    private String secondPartNumber;

    @Schema(description = "Folder number")
    private String number;

    @Schema(description = "Folder locality or object")
    private String localityOrObject;

    @Builder.Default
    @Schema(description = "Area ids")
    private Set<UUID> areaIds = new HashSet<>();

    @Builder.Default
    @Schema(description = "Concerned maritime entity ids")
    private Set<UUID> maritimeEntityIds = new HashSet<>();

    @DateTimeFormat(iso = DATE)
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC")
    @Schema(description = "Last update from date", example = "2021-01-21")
    private LocalDate lastUpdatePeriodDateStart;

    @DateTimeFormat(iso = DATE)
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC")
    @Schema(description = "Last update to date", example = "2021-01-21")
    private LocalDate lastUpdatePeriodDateEnd;

    @Schema(description = "Last update year")
    private String lastUpdateYear;

    @Schema(description = "Study Id")
    private UUID studyId;
}
