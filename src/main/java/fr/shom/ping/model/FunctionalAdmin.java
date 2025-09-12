package fr.shom.ping.model;

import fr.shom.ping.model.dto.CreateFunctionalAdminDto;
import fr.shom.ping.visitor.ProUserConvertVisitor;
import fr.shom.ping.visitor.ProUserEditDtoVisitor;
import java.io.Serial;
import java.util.UUID;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import static fr.shom.ping.model.enums.ProUserRole.FUNCTIONAL_ADMIN;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@SuperBuilder
@Entity
@DiscriminatorValue("FUNCTIONAL_ADMIN")
public class FunctionalAdmin extends ProUser {
    @Serial
    private static final long serialVersionUID = 8461668975023142696L;

    public FunctionalAdmin(CreateFunctionalAdminDto dto, UUID keycloakId) {
        super(dto, keycloakId);
    }

    @Override
    public <T> T toEditDto(ProUserEditDtoVisitor<T> visitor) {
        return visitor.toFunctionalAdminEditDto(this);
    }

    public FunctionalAdmin(ServiceUser serviceUser) {
        super(serviceUser);
        setRole(FUNCTIONAL_ADMIN);
    }

    @Override
    public <T> T switchRole(ProUserConvertVisitor<T> visitor) {
        return visitor.toServiceUser(this);
    }

    public ServiceUser toServiceUser() {
        return new ServiceUser(this);
    }
}
