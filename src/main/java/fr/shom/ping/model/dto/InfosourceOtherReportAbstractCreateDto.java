package fr.shom.ping.model.dto;

import com.google.common.collect.ImmutableMap;
import fr.shom.ping.model.Attachment;
import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.Infosource.InfosourceBuilder;
import fr.shom.ping.model.InfosourcePart;
import fr.shom.ping.model.enums.InfosourcePartObjectKeys;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static fr.shom.ping.model.enums.InfosourceNature.OTHER_REPORT;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@SuperBuilder
@Schema(name = "Infosource Create Dto (Other Report)")
public class InfosourceOtherReportAbstractCreateDto extends InfosourceReportAbstractCreateDto {
    @Serial
    private static final long serialVersionUID = 7553869605332524547L;

    protected Instant date;

    public InfosourceOtherReportAbstractCreateDto(Infosource infosource) {
        super(infosource);
        final Map<String, Object> infosourcePartObject = infosource.getInfosourceParts().first().getObject();
        if (infosourcePartObject != null) {
            this.date = Optional.ofNullable(infosourcePartObject.get(InfosourcePartObjectKeys.OBSERVATION_DATE.getKeyName()))
                .map(date -> Instant.parse(date.toString()))
                .orElse(null);
        }
    }

    public InfosourceBuilder toInfosourceBuilder(List<Attachment> attachments) {
        return Infosource.builder()
            .infosourceParts(new TreeSet<>(singletonList(
                InfosourcePart.builder()
                    .position(1)
                    .localisation(localisation.stream().map(FeatureDto::toFeature).collect(toSet()))
                    .description(description)
                    .object(
                        ofNullable(date)
                            .map(date -> ImmutableMap.of(InfosourcePartObjectKeys.OBSERVATION_DATE.getKeyName(), (Object) date.toString()))
                            .orElse(null))
                    .build()
            )))
            .nature(OTHER_REPORT)
            .timezone(timezone)
            .attachments(new HashSet<>(attachments));
    }
}
