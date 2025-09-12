package fr.shom.ping.model.dto;

import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.enums.ServiceRole;
import fr.shom.ping.visitor.ProUserVisitor;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@Schema(name = "CreateServiceUserDto Model")
public class CreateServiceUserDto extends CreateProUserDto<ServiceUser> implements Serializable {
    @Serial
    private static final long serialVersionUID = -560336367882098558L;

    @Schema(description = "Service ID", required = true)
    @NotNull(message = "validation.create-service-user.service-id.not-null")
    private UUID serviceId;

    @Schema(description = "Role", required = true)
    @NotNull(message = "validation.create-service-user.role.not-null")
    private ServiceRole serviceRole;

    @Override
    public ServiceUser toModel(ProUserVisitor visitor, UUID keycloakId) {
        return visitor.toProUserModel(this, keycloakId);
    }

    @Override
    public ServiceUser update(ProUserVisitor visitor, ServiceUser proUser) {
        return visitor.update(this, proUser);
    }
}
