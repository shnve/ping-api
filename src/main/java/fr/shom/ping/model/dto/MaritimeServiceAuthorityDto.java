package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.INTERNATIONAL_NAME_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.NAME_DESCRIPTION;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
@Builder
@Schema(name = "Maritime Service Authority DTO")
public class MaritimeServiceAuthorityDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 5648038938013368635L;

    @Schema(description = ID_DESCRIPTION)
    private UUID id;

    @Schema(description = NAME_DESCRIPTION)
    @NoHtml
    private String name;

    @Schema(description = INTERNATIONAL_NAME_DESCRIPTION)
    @NoHtml
    private String internationalName;

    @NoHtml
    private String authorityCode;
}
