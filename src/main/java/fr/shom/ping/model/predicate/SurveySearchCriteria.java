package fr.shom.ping.model.predicate;

import com.fasterxml.jackson.annotation.JsonFormat;
import fr.shom.ping.model.enums.SurveyState;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.format.annotation.DateTimeFormat;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Schema(name = "Survey search criteria")
public class SurveySearchCriteria {
    @Schema(description = "State in table (test, draft, transmitted)", required = true)
    @NotNull
    protected SurveyState state;

    protected String identifier;

    protected String nameAndLocation;

    @DateTimeFormat(iso = DATE)
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC")
    @Schema(description = "Survey start date", example = "2021-01-21")
    protected LocalDate startDate;

    @DateTimeFormat(iso = DATE)
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC")
    @Schema(description = "Survey end date", example = "2021-01-21")
    protected LocalDate endDate;

    @DateTimeFormat(iso = DATE)
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC")
    @Schema(description = "Survey start date", example = "2021-01-21")
    protected LocalDate transmissionStartDate;

    @DateTimeFormat(iso = DATE)
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC")
    @Schema(description = "Survey end date", example = "2021-01-21")
    protected LocalDate transmissionEndDate;
}
