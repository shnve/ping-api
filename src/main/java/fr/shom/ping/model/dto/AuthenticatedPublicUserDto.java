package fr.shom.ping.model.dto;


import fr.shom.ping.model.Area;
import fr.shom.ping.model.PublicUser;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.util.Optional;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Schema(name = "AuthenticatedPublicUserDto Model")
public class AuthenticatedPublicUserDto extends AuthenticatedUserDto {
    @Serial
    private static final long serialVersionUID = -7323783058467608739L;

    @Schema(description = "Is pro account")
    private boolean proAccount;

    public static AuthenticatedPublicUserDto fromPublicUser(PublicUser publicUser) {
        return AuthenticatedPublicUserDto.builder()
            .id(publicUser.getId())
            .title(publicUser.getTitle())
            .firstname(publicUser.getFirstname())
            .lastname(publicUser.getLastname())
            .preferredAreaId(Optional.ofNullable(publicUser.getPreferredArea()).map(Area::getId).orElse(null))
            .hasNotificationsSound(publicUser.hasNotificationsSound())
            .proAccount(publicUser.isProAccount())
            .build();
    }


}
