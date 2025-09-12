package fr.shom.ping.model.dto;

import fr.shom.ping.model.Attachment;
import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.Infosource.InfosourceBuilder;
import fr.shom.ping.model.InfosourcePart;
import fr.shom.ping.model.enums.InfosourcePartObjectKeys;
import fr.shom.ping.model.enums.MarkupType;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static fr.shom.ping.model.enums.InfosourceNature.HAZARD_REPORT;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@SuperBuilder
@Schema(name = "Infosource Create Dto (Hazard Report)")
public class InfosourceHazardReportAbstractCreateDto extends InfosourceReportAbstractCreateDto {
    @Serial
    private static final long serialVersionUID = 1844448224326672181L;

    @NotNull(message = "validation.infosource.hazard-report.markup-type.not-null")
    private MarkupType type;

    @NoHtml
    private String objectName;

    protected Instant date;

    public InfosourceHazardReportAbstractCreateDto(Infosource infosource) {
        super(infosource);

        final Map<String, Object> infosourcePartObject = infosource.getInfosourceParts().first().getObject();
        this.type = MarkupType.valueOf(infosourcePartObject.get(InfosourcePartObjectKeys.MARKUP_TYPE.getKeyName()).toString());
        this.objectName = Optional.ofNullable(infosourcePartObject.get(InfosourcePartObjectKeys.OBJECT_NAME.getKeyName()))
            .map(Objects::toString)
            .orElse(null);
        this.date = Optional.ofNullable(infosourcePartObject.get(InfosourcePartObjectKeys.OBSERVATION_DATE.getKeyName()))
            .map(date -> Instant.parse(date.toString()))
            .orElse(null);
    }

    public InfosourceBuilder toInfosourceBuilder(List<Attachment> attachments) {
        Map<String, Object> object = new HashMap<>();
        object.put(InfosourcePartObjectKeys.MARKUP_TYPE.getKeyName(), type);

        if (date != null) {
            object.put(InfosourcePartObjectKeys.OBSERVATION_DATE.getKeyName(), date.toString());
        }

        if (objectName != null) {
            object.put(InfosourcePartObjectKeys.OBJECT_NAME.getKeyName(), objectName);
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
            .nature(HAZARD_REPORT)
            .timezone(timezone)
            .attachments(new HashSet<>(attachments));
    }
}
