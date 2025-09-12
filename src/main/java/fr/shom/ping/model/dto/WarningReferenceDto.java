package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Warning;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(NON_NULL)
public class WarningReferenceDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -2269401846102315096L;

    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @Schema(description = "Name of series", required = true)
    @NoHtml
    private String nameOfSeries;

    @Schema(description = "Warning number", required = true)
    private Integer warningNumber;

    @Schema(description = "Year", required = true)
    private Integer year;

    @Schema(description = "Creation date", required = true)
    private Instant creationDate;

    @Schema(description = "Publication time", required = true)
    private Instant publicationTime;

    public static WarningReferenceDto fromWarning(Warning warning) {
        return WarningReferenceDto.builder()
            .id(warning.getId())
            .nameOfSeries(warning.getNameOfSeries())
            .warningNumber(warning.getWarningNumber())
            .year(warning.getYear())
            .creationDate(warning.getCreationDate())
            .publicationTime(warning.getPublicationTime())
            .build();
    }
}
