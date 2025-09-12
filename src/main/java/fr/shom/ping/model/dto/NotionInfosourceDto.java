package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.Warning;
import fr.shom.ping.model.enums.InfosourceStatus;
import java.io.Serial;
import java.time.Instant;
import fr.shom.ping.validation.NoHtml;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
public class NotionInfosourceDto extends NotionDetailsDto {
    @Serial
    private static final long serialVersionUID = -5751877681604531687L;

    @NoHtml
    private String number;
    private InfosourceStatus status;
    private Instant lastUpdate;

    public NotionInfosourceDto(Infosource infosource) {
        id = infosource.getId();
        number = infosource.getNumber();
        status = infosource.getStatus();
        creationDate = infosource.getCreationDate();
        lastUpdate = infosource.getLastUpdate();
        linkedNotionIds = infosource.getLinkedWarnings().stream()
            .map(Warning::getId)
            .collect(toSet());
        ofNullable(infosource.getParentInfosource()).ifPresent(parentInfosource -> linkedNotionIds.add(parentInfosource.getId()));
    }
}
