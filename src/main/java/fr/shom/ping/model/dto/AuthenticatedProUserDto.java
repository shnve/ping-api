package fr.shom.ping.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@Schema(name = "AuthenticatedServiceUserDto Model")
public abstract class AuthenticatedProUserDto extends AuthenticatedUserDto {
    @Serial
    private static final long serialVersionUID = 5605111180889990715L;

    @Schema(description = ID_DESCRIPTION)
    private UUID id;

    @Schema(description = "Role")
    private String role;

}
