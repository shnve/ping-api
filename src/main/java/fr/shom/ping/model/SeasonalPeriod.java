package fr.shom.ping.model;

import java.io.Serial;
import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
@Builder
public class SeasonalPeriod implements Serializable, Comparable<SeasonalPeriod> {
    @Serial
    private static final long serialVersionUID = 411784921607595234L;

    @Column(nullable = false)
    @Default
    private Integer position = 1;

    @Min(value = 1, message = "validation.georegulation.seasonal-period.day-start.invalid")
    @Max(value = 31, message = "validation.georegulation.seasonal-period.day-start.invalid")
    private Integer dayStart;

    @Min(value = 1, message = "validation.georegulation.seasonal-period.month-start.invalid")
    @Max(value = 12, message = "validation.georegulation.seasonal-period.month-start.invalid")
    private Integer monthStart;

    @Min(value = 1, message = "validation.georegulation.seasonal-period.day-end.invalid")
    @Max(value = 31, message = "validation.georegulation.seasonal-period.day-end.invalid")
    private Integer dayEnd;

    @Min(value = 1, message = "validation.georegulation.seasonal-period.month-end.invalid")
    @Max(value = 12, message = "validation.georegulation.seasonal-period.month-end.invalid")
    private Integer monthEnd;

    @Override
    public int compareTo(SeasonalPeriod seasonalPeriod) {
        return position.compareTo(seasonalPeriod.getPosition());
    }
}
