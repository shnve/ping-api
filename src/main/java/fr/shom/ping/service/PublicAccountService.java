package fr.shom.ping.service;

import fr.shom.ping.model.PublicUser;
import fr.shom.ping.repository.UserRepository;
import org.keycloak.admin.client.resource.RealmResource;
import org.springframework.stereotype.Service;

@Service
public class PublicAccountService extends AbstractAccountService<PublicUser> {
    public PublicAccountService(RealmResource publicRealmResource, UserRepository repository) {
        super(publicRealmResource, repository);
    }
}
