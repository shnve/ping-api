package fr.shom.ping.model.dto;

import com.google.common.collect.ImmutableMap;
import fr.shom.ping.model.Attachment;
import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.InfosourcePart;
import fr.shom.ping.model.enums.InfosourcePartObjectKeys;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static fr.shom.ping.model.enums.InfosourceNature.FUTURE_INFORMATION;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@SuperBuilder
@Schema(name = "Infosource Create Dto (Future Updates)")
public class InfosourceFutureUpdatesAbstractCreateDto extends InfosourceReportAbstractCreateDto {
    @Serial
    private static final long serialVersionUID = 4932689405000479173L;

    @NotEmpty(message = "validation.infosource.future-updates.object-future-info.not-empty")
    @NoHtml
    protected String objectFutureInfo;

    public InfosourceFutureUpdatesAbstractCreateDto(Infosource infosource) {
        super(infosource);
        this.objectFutureInfo =
            Optional.ofNullable(infosource.getInfosourceParts().first().getObject().get(InfosourcePartObjectKeys.OBJECT_FUTURE_INFO.getKeyName()))
                .map(Objects::toString)
                .orElse(null);
    }

    public Infosource.InfosourceBuilder toInfosourceBuilder(List<Attachment> attachments) {
        return Infosource.builder()
            .infosourceParts(new TreeSet<>(singletonList(
                InfosourcePart.builder()
                    .position(1)
                    .localisation(getLocalisation().stream().map(FeatureDto::toFeature).collect(toSet()))
                    .description(getDescription())
                    .object(ImmutableMap.of(InfosourcePartObjectKeys.OBJECT_FUTURE_INFO.getKeyName(), objectFutureInfo))
                    .build()
            )))
            .nature(FUTURE_INFORMATION)
            .attachments(new HashSet<>(attachments));
    }
}
