package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.validation.NoHtml;
import fr.shom.ping.validation.ValidTimezone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.utils.SchemaUtil.FIXED_DATE_RANGE_TABLE;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = NON_NULL)
@Schema(name = "FixedDateRange Model")
@Embeddable
@Table(name = FIXED_DATE_RANGE_TABLE)
public class FixedDateRange implements Serializable, Comparable<FixedDateRange>, Cloneable {
    @Serial
    private static final long serialVersionUID = -5612129884497426878L;

    @Default
    @JsonIgnore
    private Integer position = 1;

    @Schema(description = "Date end")
    private LocalDate dateEnd;

    @Schema(description = "Date start")
    private LocalDate dateStart;

    @Schema(description = "Time of day end. Only present if dateEnd is also present.")
    private LocalTime timeOfDayEnd;

    @Schema(description = "Time of day start. Only present if dateStart is also present.")
    private LocalTime timeOfDayStart;

    @Schema(description = "Timezone")
    @ValidTimezone(message = "validation.fixed-date-range.timezone.invalid")
    @NoHtml
    private String timezone;

    @Override
    public int compareTo(FixedDateRange fixedDateRange) {
        return position.compareTo(fixedDateRange.getPosition());
    }

    @Override
    public FixedDateRange clone() {
        return FixedDateRange.builder()
            .dateStart(this.getDateStart())
            .timeOfDayStart(this.getTimeOfDayStart())
            .dateEnd(this.getDateEnd())
            .timeOfDayEnd(this.getTimeOfDayEnd())
            .timezone(this.getTimezone())
            .build();
    }
}
