package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.Warning;
import fr.shom.ping.model.WarningReference;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.time.Instant;
import java.util.Collection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.util.stream.Collectors.toSet;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
public class NotionWarningDto extends NotionDetailsDto {
    @Serial
    private static final long serialVersionUID = -4988889685095874376L;

    @Schema(description = "Name of series", required = true)
    @NoHtml
    private String nameOfSeries;

    @Schema(description = "Warning number", required = true)
    private Integer warningNumber;

    @Schema(description = "Year", required = true)
    private Integer year;

    @Schema(description = "Publication time", required = true)
    private Instant publicationTime;

    public NotionWarningDto(Warning warning) {
        id = warning.getId();
        nameOfSeries = warning.getNameOfSeries();
        warningNumber = warning.getWarningNumber();
        year = warning.getYear();
        creationDate = warning.getCreationDate();
        publicationTime = warning.getPublicationTime();
        linkedNotionIds = warning.getWarningReferences().stream()
            .map(WarningReference::getWarnings)
            .flatMap(Collection::stream)
            .map(Warning::getId)
            .collect(toSet());
        linkedNotionIds.addAll(warning.getInfosources().stream()
            .map(Infosource::getId)
            .collect(toSet()));
    }
}
