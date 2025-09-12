package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.enums.ServiceRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(NON_NULL)
@Schema(name = "User info assign to maritime service")
public class MaritimeServiceUserDto {
    @Schema(description = ID_DESCRIPTION)
    private UUID serviceUserId;

    @Schema(description = "Role assign to the service")
    private ServiceRole serviceRole;

    @Schema(description = "Email")
    @Email
    private String email;

    @Schema(description = "Service attached to the user")
    private UUID serviceId;
}
