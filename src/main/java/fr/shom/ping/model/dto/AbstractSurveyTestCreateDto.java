package fr.shom.ping.model.dto;

import fr.shom.ping.model.Area;
import fr.shom.ping.model.Attachment;
import fr.shom.ping.model.FixedDateRange;
import fr.shom.ping.model.Survey;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static java.time.ZoneOffset.UTC;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@SuperBuilder
@Schema(name = "Survey Deposit Create Dto Model")
public class AbstractSurveyTestCreateDto extends AbstractSurveyCreateDto {
    @Serial
    private static final long serialVersionUID = 7549404188267216307L;

    @Schema(description = "Achievement period")
    protected FixedDateRange achievementPeriod;

    public Survey.SurveyBuilder toSurveyBuilder(Area area, List<Attachment> bathyAttachments) {
        final Survey.SurveyBuilder builder = super.toSurveyBuilder(area, bathyAttachments);
        if (achievementPeriod != null && achievementPeriod.getDateStart() != null && achievementPeriod.getDateEnd() != null) {
            builder.startDate(achievementPeriod.getDateStart().atStartOfDay().toInstant(UTC))
                .endDate(achievementPeriod.getDateEnd().atStartOfDay().toInstant(UTC));
        }
        return builder;
    }
}
