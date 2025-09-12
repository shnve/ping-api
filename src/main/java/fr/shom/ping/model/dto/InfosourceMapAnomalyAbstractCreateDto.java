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

import static fr.shom.ping.model.enums.InfosourceNature.MAP_ANOMALY;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@SuperBuilder
@Schema(name = "Infosource Create Dto (Map Anomaly)")
public class InfosourceMapAnomalyAbstractCreateDto extends InfosourceReportAbstractCreateDto {
    @Serial
    private static final long serialVersionUID = 7911037652698791532L;

    @NotEmpty(message = "validation.infosource.map-anomaly.map-number.not-empty")
    @NoHtml
    private String mapNumber;

    @NoHtml
    private String editionNumber;

    @NoHtml
    private String lastPatchNumber;

    protected Instant date;

    public InfosourceMapAnomalyAbstractCreateDto(Infosource infosource) {
        super(infosource);
        final Map<String, Object> infosourcePartObject = infosource.getInfosourceParts().first().getObject();
        this.mapNumber = Optional.ofNullable(infosourcePartObject.get(InfosourcePartObjectKeys.MAP_NUMBER.getKeyName()))
            .map(Objects::toString)
            .orElse(null);
        this.editionNumber = Optional.ofNullable(infosourcePartObject.get(InfosourcePartObjectKeys.EDITION_NUMBER.getKeyName()))
            .map(Objects::toString)
            .orElse(null);
        this.lastPatchNumber = Optional.ofNullable(infosourcePartObject.get(InfosourcePartObjectKeys.LAST_PATCH_NUMBER.getKeyName()))
            .map(Objects::toString)
            .orElse(null);
        this.date = Optional.ofNullable(infosourcePartObject.get(InfosourcePartObjectKeys.OBSERVATION_DATE.getKeyName()))
            .map(date -> Instant.parse(date.toString()))
            .orElse(null);
    }

    public InfosourceBuilder toInfosourceBuilder(List<Attachment> attachments) {
        Map<String, Object> object = new HashMap<>();
        object.put(InfosourcePartObjectKeys.MAP_NUMBER.getKeyName(), mapNumber);

        if (date != null) {
            object.put(InfosourcePartObjectKeys.OBSERVATION_DATE.getKeyName(), date.toString());
        }

        if (isNotEmpty(editionNumber)) {
            object.put(InfosourcePartObjectKeys.EDITION_NUMBER.getKeyName(), editionNumber);
        }

        if (isNotEmpty(lastPatchNumber)) {
            object.put(InfosourcePartObjectKeys.LAST_PATCH_NUMBER.getKeyName(), lastPatchNumber);
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
            .nature(MAP_ANOMALY)
            .timezone(timezone)
            .attachments(new HashSet<>(attachments));
    }
}
