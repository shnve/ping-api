package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.enums.MaritimeServiceType;
import fr.shom.ping.model.enums.ServiceRole;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(name = "Service role link to a service user")
public class ServiceLinkToUserDto {
    @Schema(description = ID_DESCRIPTION)
    private UUID id;

    @Schema(description = "Role assign to the service")
    private ServiceRole serviceRole;

    @Schema(description = "Service maritime name")
    @NoHtml
    private String name;

    @Schema(description = "Service maritime international name")
    @NoHtml
    private String internationalName;

    @Schema(description = "Service maritime type")
    private MaritimeServiceType type;

    @Schema(description = "Survey owner")
    protected boolean surveyOwner;
}
