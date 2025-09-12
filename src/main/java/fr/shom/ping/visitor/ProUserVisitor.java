package fr.shom.ping.visitor;

import fr.shom.ping.model.FunctionalAdmin;
import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.dto.CreateFunctionalAdminDto;
import fr.shom.ping.model.dto.CreateServiceUserDto;
import java.util.UUID;

public interface ProUserVisitor {
    ServiceUser toProUserModel(CreateServiceUserDto dto, UUID keycloakId);

    ServiceUser update(CreateServiceUserDto dto, ServiceUser serviceUser);

    FunctionalAdmin toProUserModel(CreateFunctionalAdminDto dto, UUID keycloakId);

    FunctionalAdmin update(CreateFunctionalAdminDto dto, FunctionalAdmin functionalAdmin);
}
