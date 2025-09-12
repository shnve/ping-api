package fr.shom.ping.controller;

import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.dto.ServiceLinkToUserDto;
import fr.shom.ping.service.ServiceUserService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/service-users")
@RequiredArgsConstructor
@Tag(name = "User (Service)", description = "Service User API")
public class ServiceUserController {

    private final ServiceUserService serviceUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('functional-admin') OR hasAuthority('service-user') and hasPermission(#serviceId, 'SERVICE_ADMIN')")
    public List<ServiceUser> findAll(@RequestParam UUID serviceId, @Parameter(hidden = true) Principal principal) {
        return this.serviceUserService.findAll(serviceId, principal.getName());
    }

    @GetMapping("/{id}/services")
    public List<ServiceLinkToUserDto> userServicesLinked(@PathVariable UUID id) {
        return this.serviceUserService.findServicesAssignToUser(id);
    }
}
