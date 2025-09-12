package fr.shom.ping.visitor;

import fr.shom.ping.model.Area;
import fr.shom.ping.model.FunctionalAdmin;
import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.PhoneNumber;
import fr.shom.ping.model.ProUser;
import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.ServiceUserRole;
import fr.shom.ping.model.dto.CreateFunctionalAdminDto;
import fr.shom.ping.model.dto.CreateProUserDto;
import fr.shom.ping.model.dto.CreateServiceUserDto;
import fr.shom.ping.model.dto.PhoneNumberDto;
import fr.shom.ping.repository.ServiceUserRepository;
import fr.shom.ping.repository.UserRepository;
import fr.shom.ping.service.AreaService;
import fr.shom.ping.service.MaritimeServiceService;
import fr.shom.ping.utils.InfosourceUtil;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class ProUserVisitorImpl implements ProUserVisitor {

    private final MaritimeServiceService maritimeServiceService;
    private final AreaService areaService;

    @Override
    public ServiceUser toProUserModel(CreateServiceUserDto dto, UUID keycloakId) {
        MaritimeService service = maritimeServiceService.findById(dto.getServiceId());
        return new ServiceUser(dto, keycloakId, service);
    }

    @Override
    public ServiceUser update(CreateServiceUserDto dto, ServiceUser serviceUser) {
        MaritimeService newService = maritimeServiceService.findById(dto.getServiceId());
        updateModelFromDto(serviceUser, dto);

        // Get the current service
        MaritimeService currentService = ofNullable(serviceUser.getService()).orElse(newService);
        serviceUser.setService(newService);
        // Get all roles by service except the previous service
        List<ServiceUserRole> previousRolesWithoutPreviousService = serviceUser.getRoles().stream()
            .filter(serviceUserRole -> !serviceUserRole.getServiceId().equals(currentService.getId()))
            .collect(toList());
        // Add new role for new service to previous role list
        previousRolesWithoutPreviousService.add(ServiceUserRole.builder().serviceRole(dto.getServiceRole()).serviceId(newService.getId()).build());
        // Add roles to user
        serviceUser.setRoles(newHashSet(previousRolesWithoutPreviousService));
        return serviceUser;
    }

    @Override
    public FunctionalAdmin toProUserModel(CreateFunctionalAdminDto dto, UUID keycloakId) {
        return new FunctionalAdmin(dto, keycloakId);
    }

    @Override
    public FunctionalAdmin update(CreateFunctionalAdminDto dto, FunctionalAdmin functionalAdmin) {
        updateModelFromDto(functionalAdmin, dto);
        return functionalAdmin;
    }

    private void updateModelFromDto(ProUser proUser, CreateProUserDto dto) {
        UUID preferredAreaId = dto.getPreferredAreaId();
        Area area = ofNullable(preferredAreaId).map(id -> areaService.getOne(preferredAreaId)).orElse(null);
        proUser.setTitle(dto.getTitle());
        proUser.setFirstname(dto.getFirstname());
        proUser.setLastname(dto.getLastname());
        proUser.setEmail(dto.getEmail());
        proUser.setPreferredArea(area);
        proUser.setRole(dto.getRole());
        proUser.getPhoneNumberList().clear();
        List<PhoneNumber> phoneNumbers = ((List<PhoneNumberDto>) dto.getPhoneNumbers()).stream().map(PhoneNumber::new).collect(toList());
        phoneNumbers.forEach(phoneNumber -> phoneNumber.setUser(proUser));
        proUser.getPhoneNumberList().addAll(phoneNumbers);
    }
}
