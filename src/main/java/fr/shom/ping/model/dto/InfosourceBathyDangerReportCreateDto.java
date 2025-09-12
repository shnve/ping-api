package fr.shom.ping.model.dto;

import fr.shom.ping.model.Attachment;
import fr.shom.ping.model.FixedDateRange;
import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.Infosource.InfosourceBuilder;
import fr.shom.ping.model.InfosourcePart;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.util.List;
import java.util.TreeSet;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static com.google.common.collect.Sets.newHashSet;
import static fr.shom.ping.model.enums.InfosourceNature.BATHY;
import static fr.shom.ping.utils.PingStringUtil.REGEX_PATTERN_NULL_OR_NOT_BLANK;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@SuperBuilder
@Schema(name = "Infosource Create Dto (Bathy Danger Report)")
public class InfosourceBathyDangerReportCreateDto extends InfosourceReportAbstractCreateDto {
    @Serial
    private static final long serialVersionUID = 2977729544977875892L;

    @Schema(description = "Survey period")
    @NotNull(message = "validation.survey.survey-period.not-null")
    protected FixedDateRange surveyPeriod;

    @Pattern(regexp = REGEX_PATTERN_NULL_OR_NOT_BLANK, flags = { Pattern.Flag.DOTALL }, message = "validation.infosource.bathy-danger.additional-informations.null-or-not-blank")
    @NoHtml
    private String confidentialityInformation;

    public InfosourceBathyDangerReportCreateDto(Infosource infosource) {
        super(infosource);
        this.surveyPeriod = infosource.getStartDate() != null && infosource.getEndDate() != null ?
            FixedDateRange.builder()
                .dateStart(infosource.getStartDate().atZone(UTC).toLocalDate())
                .dateEnd(infosource.getEndDate().atZone(UTC).toLocalDate())
                .build()
            : null;
        this.confidentialityInformation = infosource.getConfidentialityInformation();
    }

    public InfosourceBuilder toInfosourceBuilder(List<Attachment> attachments) {
        return Infosource.builder()
            .infosourceParts(new TreeSet<>(singletonList(
                InfosourcePart.builder()
                    .position(1)
                    .description(description)
                    .localisation(localisation.stream().map(FeatureDto::toFeature).collect(toSet()))
                    .build()
            )))
            .confidentialityInformation(confidentialityInformation)
            .startDate(surveyPeriod.getDateStart().atStartOfDay().toInstant(UTC))
            .endDate(surveyPeriod.getDateEnd().atStartOfDay().toInstant(UTC))
            .attachments(newHashSet(attachments))
            .nature(BATHY);
    }
}
