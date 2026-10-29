package fr.shom.ping.service;

import fr.shom.ping.exception.KeycloakUserCreationException;
import fr.shom.ping.model.KeycloakError;
import fr.shom.ping.model.PublicUser;
import fr.shom.ping.model.dto.CreateProUserDto;
import fr.shom.ping.model.dto.CreatePublicUserDto;
import fr.shom.ping.model.enums.ProUserRole;
import fr.shom.ping.properties.KeycloakProperties;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import static fr.shom.ping.model.enums.ProUserRole.FUNCTIONAL_ADMIN;
import static fr.shom.ping.model.enums.UserCreationErrorMessage.fromKeycloakError;
import static java.util.Collections.singletonList;
import static org.springframework.http.HttpStatus.CREATED;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakService {
    private final KeycloakProperties keycloakProperties;
    private final RealmResource publicRealmResource;

    @Qualifier("proRealmResource")
    private final RealmResource proRealmResource;

    public UUID createUser(CreatePublicUserDto user) {
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setUsername(user.getEmail());
        userRepresentation.setEmail(user.getEmail());
        userRepresentation.setEmailVerified(false);
        userRepresentation.setEnabled(true);
        userRepresentation.singleAttribute("locale", "es");
        userRepresentation.setRequiredActions(singletonList("VERIFY_EMAIL"));
        CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
        credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
        credentialRepresentation.setValue(user.getPassword());
        userRepresentation.setCredentials(singletonList(credentialRepresentation));
        Response response = publicRealmResource.users().create(userRepresentation);
        if (response.getStatus() != CREATED.value()) {
            KeycloakError keycloakError = response.readEntity(KeycloakError.class);
            throw new KeycloakUserCreationException(fromKeycloakError(keycloakError));
        }

        return UUID.fromString(CreatedResponseUtil.getCreatedId(response));
    }

    public UUID createUser(CreateProUserDto user) {
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setUsername(user.getEmail());
        userRepresentation.setEmail(user.getEmail());
        userRepresentation.setEmailVerified(true);
        userRepresentation.setEnabled(true);
        userRepresentation.singleAttribute("locale", "es");
        Response response = proRealmResource.users().create(userRepresentation);
        if (response.getStatus() != CREATED.value()) {
            KeycloakError keycloakError = response.readEntity(KeycloakError.class);
            throw new KeycloakUserCreationException(fromKeycloakError(keycloakError));
        }

        String keycloakId = CreatedResponseUtil.getCreatedId(response);

        String pingApiClientId = keycloakProperties.getProRealm().getApiClient().getClientId();
        ClientRepresentation pingApiClient = proRealmResource.clients()
            .findByClientId(pingApiClientId)
            .getFirst();
        RoleRepresentation roleRepresentation = proRealmResource.clients()
            .get(pingApiClient.getId())
            .roles()
            .get(user.getRole() == FUNCTIONAL_ADMIN ? keycloakProperties.getRoles().getFunctionalAdmin() : keycloakProperties.getRoles().getServiceUser())
            .toRepresentation();
        proRealmResource.users()
            .get(keycloakId)
            .roles()
            .clientLevel(pingApiClient.getId())
            .add(singletonList(roleRepresentation));

        try {
            sendUpdatePasswordEmail(UUID.fromString(keycloakId));
            log.info("Correo de configuracion de contrasena enviado automaticamente al usuario profesional: {}", user.getEmail());
        } catch (Exception e) {
            log.error("No se pudo enviar el correo de configuracion de contrasena al usuario profesional {}: {}", user.getEmail(), e.getMessage());
        }

        return UUID.fromString(keycloakId);
    }

    public void deactivatePublicUser(UUID keycloakId) {
        setPublicUserEnabled(keycloakId, false);
    }

    public void activatePublicUser(UUID keycloakId) {
        setPublicUserEnabled(keycloakId, true);
    }

    private void setPublicUserEnabled(UUID keycloakId, boolean enabled) {
        setUserEnabled(publicRealmResource.users().get(keycloakId.toString()), enabled);
    }

    public void deactivateProUser(UUID keycloakId) {
        setProUserEnabled(keycloakId, false);
    }

    public void activateProUser(UUID keycloakId) {
        setProUserEnabled(keycloakId, true);
    }

    private void setProUserEnabled(UUID keycloakId, boolean enabled) {
        setUserEnabled(proRealmResource.users().get(keycloakId.toString()), enabled);
    }

    private void setUserEnabled(UserResource userResource, boolean enabled) {
        final UserRepresentation userRepresentation = userResource.toRepresentation();

        userRepresentation.setEnabled(enabled);
        userResource.update(userRepresentation);
    }

    public void updateUserEmail(UUID keycloakId, String email) {
        UserResource userResource = proRealmResource.users().get(keycloakId.toString());

        UserRepresentation userRepresentation = userResource.toRepresentation();
        userRepresentation.setUsername(email);
        userRepresentation.setEmail(email);

        userResource.update(userRepresentation);
    }

    public void updateUserRole(UUID keycloakId, ProUserRole newRole) {
        String pingApiClientId = keycloakProperties.getProRealm().getApiClient().getClientId();
        ClientRepresentation pingApiClient = proRealmResource.clients()
            .findByClientId(pingApiClientId)
            .getFirst();

        // Delete previous roles
        List<RoleRepresentation> roleRepresentations = proRealmResource.users()
            .get(keycloakId.toString())
            .roles()
            .clientLevel(pingApiClient.getId())
            .listEffective();

        proRealmResource.users()
            .get(keycloakId.toString())
            .roles()
            .clientLevel(pingApiClient.getId())
            .remove(roleRepresentations);

        // Add new role
        RoleRepresentation roleRepresentation = proRealmResource.clients()
            .get(pingApiClient.getId())
            .roles()
            .get(newRole == FUNCTIONAL_ADMIN ? keycloakProperties.getRoles().getFunctionalAdmin() : keycloakProperties.getRoles().getServiceUser())
            .toRepresentation();

        proRealmResource.users()
            .get(keycloakId.toString())
            .roles()
            .clientLevel(pingApiClient.getId())
            .add(singletonList(roleRepresentation));
    }

    public void sendUpdatePasswordEmail(UUID keycloakId) {
        UserResource userResource = proRealmResource.users().get(keycloakId.toString());
        try {
            UserRepresentation rep = userResource.toRepresentation();
            if (rep.getAttributes() == null || !rep.getAttributes().containsKey("locale")) {
                rep.singleAttribute("locale", "es");
                userResource.update(rep);
            }
        } catch (Exception e) {
            log.warn("No se pudo asegurar locale es para usuario {}: {}", keycloakId, e.getMessage());
        }
        userResource.executeActionsEmail(singletonList("UPDATE_PASSWORD"));
    }

    public void sendUpdatePasswordEmailPublicUser(UUID keycloakId) {
        UserResource userResource = publicRealmResource.users().get(keycloakId.toString());
        userResource.executeActionsEmail(singletonList("UPDATE_PASSWORD"));
    }

    public void updateEmailPublicUser(PublicUser model, String newEmail) {
        UserResource userResource = publicRealmResource.users().get(model.getKeycloakId().toString());
        UserRepresentation userRepresentation = userResource.toRepresentation();
        userRepresentation.setEmail(newEmail);
        userRepresentation.setUsername(newEmail);
        userRepresentation.setEmailVerified(false);
        userRepresentation.setEnabled(true);
        userRepresentation.singleAttribute("locale", "es");
        userRepresentation.setRequiredActions(singletonList("VERIFY_EMAIL"));
        userResource.update(userRepresentation);
    }
}
