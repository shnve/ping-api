package fr.shom.ping.service;

import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.model.ProUser;
import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.ServiceUserRole;
import fr.shom.ping.model.dto.AuthenticatedServiceUserDto;
import fr.shom.ping.repository.ProUserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import static fr.shom.ping.model.enums.ServiceRole.SERVICE_ADMIN;
import static fr.shom.ping.model.enums.UserStatus.DISABLED;
import static fr.shom.ping.model.enums.UserStatus.ENABLED;

@RequiredArgsConstructor
public abstract class AbstractProUserService {
    protected final ProUserRepository proUserRepository;
    protected final KeycloakService keycloakService;

    @Transactional
    public void disable(UUID id) {
        final ProUser user = proUserRepository.findById(id).orElseThrow(NotFoundException::new);
        final UUID keycloakId = proUserRepository.findKeycloakIdByEmail(user.getEmail()).orElseThrow(NotFoundException::new);
        keycloakService.deactivateProUser(keycloakId);
        proUserRepository.updateUserStatusByEmail(DISABLED, user.getEmail());
    }

    @Transactional
    public void enable(UUID id) {
        final ProUser user = proUserRepository.findById(id).orElseThrow(NotFoundException::new);
        final UUID keycloakId = proUserRepository.findKeycloakIdByEmail(user.getEmail()).orElseThrow(NotFoundException::new);
        keycloakService.activateProUser(keycloakId);
        proUserRepository.updateUserStatusByEmail(ENABLED, user.getEmail());
    }

    public static List<UUID> getServiceIdsOnWhichUserIsAdmin(ServiceUser proUser) {
        final AuthenticatedServiceUserDto serviceUser = AuthenticatedServiceUserDto.fromServiceUser(proUser);
        return serviceUser.getServiceRoles().stream()
            .filter(serviceUserRole -> SERVICE_ADMIN == serviceUserRole.getServiceRole())
            .map(ServiceUserRole::getServiceId)
            .toList();
    }
}
