package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Georegulation;
import fr.shom.ping.utils.GeoregulationUtil;
import java.io.Serial;
import java.time.Instant;
import java.util.Set;
import fr.shom.ping.validation.NoHtml;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Optional.ofNullable;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
public class NotionGeoregulationDto extends NotionDetailsDto {
    @Serial
    private static final long serialVersionUID = 5769040495869236583L;

    @NoHtml
    private String number;

    private Instant publicationDate;

    public NotionGeoregulationDto(Georegulation georegulation) {
        id = georegulation.getId();
        number = GeoregulationUtil.getNumber(
            georegulation.getNumber().getYear(),
            georegulation.getNumber().getText(),
            Set.copyOf(georegulation.getNumber().getAuthorityCodes()),
            georegulation.getNumber().getDate());
        creationDate = georegulation.getCreationDate();
        publicationDate = georegulation.getPublicationDate();
        ofNullable(georegulation.getInfosource()).ifPresent(infosource -> linkedNotionIds = newHashSet(infosource.getId()));
    }
}
