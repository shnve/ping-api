package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;

@Data
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Survey Dto Model")
public abstract class AbstractSurveyDto implements Serializable  {
    @Serial
    private static final long serialVersionUID = -4858216895834034967L;
    @Schema(description = ID_DESCRIPTION, required = true)
    protected UUID id;

    protected String identifier;

    protected String nameAndLocation;

    protected Instant lastUpdate;

    protected Instant startDate;

    protected Instant endDate;
}
