package fr.shom.ping.model.predicate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.enums.SurveyTransmissionStatus;
import fr.shom.ping.validation.ValueOfEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
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
@Schema(name = "Survey admin BRI search criteria")
public class SurveyAdminBriSearchCriteria extends SurveySearchCriteria {
    @Schema(description = "SHOM identifier (contains)")
    private String shomId;

    @Schema(description = "Owner organization (contains)")
    private String ownerOrganization;

    @Schema(description = "Producer organization (contains)")
    private String producerOrganization;

    @Schema(description = "Survey is not compliant")
    private Boolean notCompliant;

    @Schema(description = "Transmission status (equals)")
    @ValueOfEnum(enumClass = SurveyTransmissionStatus.class, message = "validation.search.transmission-status.not-in-enum")
    private String transmissionStatus;

    @DateTimeFormat(iso = DATE)
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC")
    @Schema(description = "Data expected deletion date (equals)", example = "2023-10-19")
    private LocalDate expectedDeletionDate;

    @Schema(description = "BRI internal comment (contains)")
    private String briInternalComment;
}
