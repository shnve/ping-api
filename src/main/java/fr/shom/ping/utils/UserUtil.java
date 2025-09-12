package fr.shom.ping.utils;

import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.enums.ServiceRole;
import java.util.Arrays;
import java.util.List;

import static fr.shom.ping.model.enums.ServiceRole.SERVICE_ADMIN;
import static fr.shom.ping.model.enums.ServiceRole.SERVICE_OPERATOR;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

public final class UserUtil {

    private UserUtil() {
    }

    public static boolean checkServiceUserHasNoRequiredRoleOnSerieService(ServiceUser serviceUser, MaritimeService service, ServiceRole... roles) {
        return serviceUser.getRoles().stream()
            .filter(serviceUserRole -> service.getId().equals(serviceUserRole.getServiceId()))
            .noneMatch(serviceUserRole -> Arrays.stream(roles).collect(toList()).contains(serviceUserRole.getServiceRole()));
    }

    public static boolean isBri(ServiceUser serviceUser, List<MaritimeService> servicesBri) {
        return serviceUser.getRoles().stream()
            .anyMatch(role -> servicesBri.stream().map(MaritimeService::getId).collect(toList()).contains(role.getServiceId())
                && asList(SERVICE_ADMIN, SERVICE_OPERATOR).contains(role.getServiceRole()));
    }
}
