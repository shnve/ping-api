package fr.shom.ping.model.dto;

import fr.shom.ping.model.FunctionalAdmin;
import fr.shom.ping.visitor.ProUserVisitor;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.util.UUID;
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
@Schema(name = "CreateFunctionalAdminDto Model")
public class CreateFunctionalAdminDto extends CreateProUserDto<FunctionalAdmin> {
    @Serial
    private static final long serialVersionUID = -531111060059143701L;

    @Override
    public FunctionalAdmin toModel(ProUserVisitor visitor, UUID keycloakId) {
        return visitor.toProUserModel(this, keycloakId);
    }

    @Override
    public FunctionalAdmin update(ProUserVisitor visitor, FunctionalAdmin proUser) {
        return visitor.update(this, proUser);
    }
}
