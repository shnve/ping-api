package fr.shom.ping;

import fr.shom.ping.model.enums.ServiceRole;
import fr.shom.ping.properties.KeycloakProperties;
import fr.shom.ping.repository.ServiceUserRepository;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

@Import(value = {KeycloakProperties.class})
public class PingApiPermissionEvaluator implements PermissionEvaluator {

    private final String principalNameAttribute;

    private final ServiceUserRepository serviceUserRepository;

    public PingApiPermissionEvaluator(ServiceUserRepository serviceUserRepository, KeycloakProperties keycloakProperties) {
        this.serviceUserRepository = serviceUserRepository;
        this.principalNameAttribute = keycloakProperties.getPrincipalAttribute();
    }

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || targetDomainObject == null || !(permission instanceof String)) {
            return false;
        }

        final Jwt principal = (Jwt) authentication.getPrincipal();
        List<UUID> serviceIds;
        if (targetDomainObject instanceof List) {
            serviceIds = ((List<Object>) targetDomainObject).stream().map(Object::toString).map(UUID::fromString).collect(toList());
        } else {
            serviceIds = singletonList(UUID.fromString(targetDomainObject.toString()));
        }

        if (permission.toString().equals("*")) {
            // If permission == *, we check that the user has any role for each provided service
            return serviceUserRepository.hasPermission(principal.getClaimAsString(principalNameAttribute), serviceIds) == serviceIds.size();
        } else {
            // else, we check that the user has the granted roles to the service
            List<ServiceRole> serviceRoles = Stream.of(permission.toString().split(",")).map(ServiceRole::valueOf).collect(toList());
            return serviceUserRepository.hasPermission(principal.getClaimAsString(principalNameAttribute), serviceRoles, serviceIds.get(0)) == 1;
        }
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        // unused function
        return false;
    }
}
