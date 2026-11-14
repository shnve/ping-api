package fr.shom.ping.service;

import fr.shom.ping.exception.BadRequestException;
import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.model.ProUser;
import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.ServiceUserRole;
import fr.shom.ping.model.dto.AuthenticatedServiceUserDto;
import fr.shom.ping.repository.ProUserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import static fr.shom.ping.model.enums.ServiceRole.SERVICE_ADMIN;
import static fr.shom.ping.model.enums.UserStatus.DISABLED;
import static fr.shom.ping.model.enums.UserStatus.ENABLED;

@RequiredArgsConstructor
@Slf4j
public abstract class AbstractProUserService {
    protected final ProUserRepository proUserRepository;
    protected final KeycloakService keycloakService;

    @Transactional
    public void disable(UUID id) {
        final ProUser user = proUserRepository.findById(id).orElseThrow(NotFoundException::new);
        keycloakService.deactivateProUser(resolveKeycloakId(user));
        proUserRepository.updateUserStatusByEmail(DISABLED, user.getEmail());
    }

    @Transactional
    public void enable(UUID id) {
        final ProUser user = proUserRepository.findById(id).orElseThrow(NotFoundException::new);
        keycloakService.activateProUser(resolveKeycloakId(user));
        proUserRepository.updateUserStatusByEmail(ENABLED, user.getEmail());
    }

    /**
     * PISAC: id de Keycloak vigente del usuario. Si el guardado ya no existe en Keycloak pero la cuenta se encuentra por
     * correo, se corrige el {@code keycloak_id} en PISAC. Si la cuenta no existe, se responde 400 con un mensaje claro
     * (antes, Keycloak respondía 404 y el portal mostraba un error 500).
     */
    protected UUID resolveKeycloakId(ProUser user) {
        final UUID keycloakId = keycloakService.findProUserKeycloakId(user.getKeycloakId(), user.getEmail())
            .orElseThrow(() -> {
                log.error("El usuario profesional {} no tiene cuenta en Keycloak (keycloak_id guardado: {})", user.getEmail(), user.getKeycloakId());
                return new BadRequestException(KeycloakService.PRO_USER_KEYCLOAK_ACCOUNT_NOT_FOUND);
            });
        if (!keycloakId.equals(user.getKeycloakId())) {
            log.warn("Se corrige el keycloak_id del usuario profesional {}: {} -> {}", user.getEmail(), user.getKeycloakId(), keycloakId);
            user.setKeycloakId(keycloakId);
            proUserRepository.save(user);
        }
        return keycloakId;
    }

    public static List<UUID> getServiceIdsOnWhichUserIsAdmin(ServiceUser proUser) {
        final AuthenticatedServiceUserDto serviceUser = AuthenticatedServiceUserDto.fromServiceUser(proUser);
        return serviceUser.getServiceRoles().stream()
            .filter(serviceUserRole -> SERVICE_ADMIN == serviceUserRole.getServiceRole())
            .map(ServiceUserRole::getServiceId)
            .toList();
    }
}
