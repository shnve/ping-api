package fr.shom.ping.model.dto;

import fr.shom.ping.model.enums.UserTitle;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@Schema(name = "AuthenticatedUserDto Model")
public abstract class AuthenticatedUserDto implements Serializable {

    @Serial
    private static final long serialVersionUID = -2033808816037749338L;
    @Schema(description = "Id", required = true)
    private UUID id;
    @Schema(description = "Title", required = true)
    private UserTitle title;

    @Schema(description = "Firstname", required = true)
    private String firstname;

    @Schema(description = "Lastname", required = true)
    private String lastname;

    @Schema(description = "Preferred Area Id")
    private UUID preferredAreaId;

    @Schema(description = "Has notifications sound")
    private boolean hasNotificationsSound;
}
