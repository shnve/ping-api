package fr.shom.ping.model.dto;

import fr.shom.ping.model.Attachment;
import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.Infosource.InfosourceBuilder;
import fr.shom.ping.model.InfosourcePart;
import fr.shom.ping.model.enums.InfosourcePartObjectKeys;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static fr.shom.ping.model.enums.InfosourceNature.PUBLICATION_ANOMALY;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@SuperBuilder
@Schema(name = "Infosource Create Dto (Publication Anomaly)")
public class InfosourcePublicationAnomalyAbstractCreateDto extends InfosourceReportAbstractCreateDto {

    @Serial
    private static final long serialVersionUID = 5978943256889378738L;

    @NotEmpty(message = "validation.infosource.publication-anomaly.issue-reference.not-empty")
    @NoHtml
    private String issueReference;

    private LocalDate upToDateAt;

    @NoHtml
    private String chapterOrPage;

    protected Instant date;

    public InfosourcePublicationAnomalyAbstractCreateDto(Infosource infosource) {
        super(infosource);
        final Map<String, Object> infosourcePartObject = infosource.getInfosourceParts().first().getObject();
        this.issueReference = Optional.ofNullable(infosourcePartObject.get(InfosourcePartObjectKeys.ISSUE_REFERENCE.getKeyName()))
            .map(Objects::toString)
            .orElse(null);
        this.upToDateAt = Optional.ofNullable(infosourcePartObject.get(InfosourcePartObjectKeys.UP_TO_DATE_AT.getKeyName()))
            .map(date -> LocalDate.parse(date.toString()))
            .orElse(null);
        this.chapterOrPage = Optional.ofNullable(infosourcePartObject.get(InfosourcePartObjectKeys.CHAPTER_OR_PAGE.getKeyName()))
            .map(Objects::toString)
            .orElse(null);
        this.date = Optional.ofNullable(infosourcePartObject.get(InfosourcePartObjectKeys.OBSERVATION_DATE.getKeyName()))
            .map(date -> Instant.parse(date.toString()))
            .orElse(null);

    }

    public InfosourceBuilder toInfosourceBuilder(List<Attachment> attachments) {
        Map<String, Object> object = new HashMap<>();
        object.put(InfosourcePartObjectKeys.ISSUE_REFERENCE.getKeyName(), issueReference);

        if (date != null) {
            object.put(InfosourcePartObjectKeys.OBSERVATION_DATE.getKeyName(), date.toString());
        }

        if (upToDateAt != null) {
            object.put(InfosourcePartObjectKeys.UP_TO_DATE_AT.getKeyName(), upToDateAt.toString());
        }

        if (isNotEmpty(chapterOrPage)) {
            object.put(InfosourcePartObjectKeys.CHAPTER_OR_PAGE.getKeyName(), chapterOrPage);
        }
        return Infosource.builder()
            .infosourceParts(new TreeSet<>(singletonList(
                InfosourcePart.builder()
                    .position(1)
                    .localisation(localisation.stream().map(FeatureDto::toFeature).collect(toSet()))
                    .description(description)
                    .object(object)
                    .build()
            )))
            .nature(PUBLICATION_ANOMALY)
            .timezone(timezone)
            .attachments(new HashSet<>(attachments));
    }
}
