package fr.shom.ping.model.dto;

import fr.shom.ping.model.Area;
import fr.shom.ping.model.FunctionalAdmin;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.util.Optional;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@Schema(name = "AuthenticatedServiceUserDto Model")
public class AuthenticatedFunctionalAdminDto extends AuthenticatedProUserDto {
    @Serial
    private static final long serialVersionUID = 231766647833800291L;


    public static AuthenticatedFunctionalAdminDto fromFunctionalAdmin(FunctionalAdmin functionalAdmin) {
        return AuthenticatedFunctionalAdminDto.builder()
            .id(functionalAdmin.getId())
            .title(functionalAdmin.getTitle())
            .firstname(functionalAdmin.getFirstname())
            .lastname(functionalAdmin.getLastname())
            .role(functionalAdmin.getRole().name())
            .preferredAreaId(Optional.ofNullable(functionalAdmin.getPreferredArea()).map(Area::getId).orElse(null))
            .hasNotificationsSound(functionalAdmin.hasNotificationsSound())
            .build();
    }

}
