package fr.shom.ping.service;

import fr.shom.ping.model.User;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityNotFoundException;
import jakarta.ws.rs.BadRequestException;
import java.util.UUID;

@RequiredArgsConstructor
public abstract class AbstractAccountService<T extends User> {
    private final RealmResource realmResource;

    private final CrudRepository<T, UUID> repository;

    @Transactional
    public void editMail(UUID id, String newMail) {
        T user = this.repository.findById(id).orElseThrow(EntityNotFoundException::new);

        UserResource userResource = realmResource.users().get(user.getKeycloakId().toString());
        UserRepresentation userRepresentation = userResource.toRepresentation();
        userRepresentation.setEmail(newMail);
        userRepresentation.setUsername(newMail);
        userResource.update(userRepresentation);

        user.setEmail(newMail);

        this.repository.save(user);
    }

    @Transactional
    public void resetPassword(UUID id, String password) {
        T user = this.repository.findById(id).orElseThrow(EntityNotFoundException::new);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setValue(password);
        credential.setTemporary(false);
        credential.setType(CredentialRepresentation.PASSWORD);

        try {
            realmResource.users().get(user.getKeycloakId().toString()).resetPassword(credential);
        } catch (BadRequestException e) {
            throw new RuntimeException("error.api.password-updated");
        }
    }
}
