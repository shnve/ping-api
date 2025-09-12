package fr.shom.ping.controller;

import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.dto.MaritimeServiceAuthorityDto;
import fr.shom.ping.model.dto.MaritimeServiceCreateDto;
import fr.shom.ping.model.dto.MaritimeServiceDto;
import fr.shom.ping.model.dto.MaritimeServiceUserDto;
import fr.shom.ping.model.enums.MaritimeServiceType;
import fr.shom.ping.model.enums.ServiceRole;
import fr.shom.ping.service.MaritimeServiceService;
import fr.shom.ping.service.ServiceUserService;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/admin-api/maritime-services")
@RequiredArgsConstructor
@Tag(name = "Maritime Service (Admin)", description = "Maritime Service Admin API")
public class MaritimeServiceAdminController {
    private final MaritimeServiceService service;
    private final ServiceUserService serviceUserService;

    @PostMapping(consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @Operation(summary = "Create Maritime Service", tags = "Maritime Service (Admin)")
    @PreAuthorize("hasAuthority('functional-admin')")
    public MaritimeServiceDto create(@Validated @RequestBody MaritimeServiceCreateDto dto) {
        return service.create(dto);
    }

    @PutMapping(value = "/{id}", consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @Operation(summary = "Update Maritime Service", tags = "Maritime Service (Admin)")
    @PreAuthorize("hasAuthority('functional-admin')")
    public MaritimeServiceDto update(@PathVariable UUID id, @Validated @RequestBody MaritimeServiceCreateDto dto) {
        return service.update(id, dto);
    }

    @GetMapping(value = "/{id}/users", consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @Operation(summary = "List users link to the maritime service", tags = "Maritime Service (Admin)")
    @PreAuthorize("hasAuthority('functional-admin') or (hasAuthority('service-user') and hasPermission(#id, '*'))")
    public List<MaritimeServiceUserDto> findUsersAssign(@PathVariable UUID id) {
        return serviceUserService.findUsersAssignToService(id);
    }

    @PostMapping(value = "/{id}/users/{userId}/{role}", consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @Operation(summary = "Add role to user on maritime service", tags = "Maritime Service (Admin)")
    @PreAuthorize("hasAuthority('functional-admin') or (hasAuthority('service-user') and hasPermission(#id, '*'))")
    public void assignRole(@PathVariable UUID id, @PathVariable UUID userId, @PathVariable ServiceRole role) {
        service.assignRole(id, userId, role);
    }

    @DeleteMapping(value = "/{id}/users/{userId}/{role}")
    @Operation(summary = " Remove role to user on maritime service", tags = "Maritime Service (Admin)")
    @PreAuthorize("hasAuthority('functional-admin') or (hasAuthority('service-user') and hasPermission(#id, 'SERVICE_ADMIN'))")
    @ResponseStatus(NO_CONTENT)
    public void deleteRole(@PathVariable UUID id, @PathVariable UUID userId, @PathVariable ServiceRole role) {
        service.deleteRole(id, userId, role);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get the maritime service by id", tags = "Maritime Service")
    @PreAuthorize("hasAuthority('functional-admin') or (hasAuthority('service-user') and hasPermission(#id, '*'))")
    public MaritimeServiceDto findById(@PathVariable UUID id) {
        return service.findById(id).asDto();
    }

    @GetMapping
    @Operation(summary = "Get maritime services by ids", tags = "Maritime Service")
    @PreAuthorize("hasAuthority('service-user') and hasPermission(#serviceIds, '*')")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    public List<MaritimeServiceDto> findAllByIds(@RequestParam List<UUID> serviceIds) {
        return service.findAllByIds(serviceIds);
    }

    @GetMapping("/search")
    @Operation(summary = "Get the list of maritime services by page", tags = "Maritime Service")
    @PreAuthorize("hasAuthority('functional-admin') or hasAuthority('service-user')")
    public Page<MaritimeService> search(Pageable pageable, @RequestParam(required = false) @NoHtml String name,
        @RequestParam(required = false) List<MaritimeServiceType> type, @RequestParam(required = false) List<UUID> areas) {
        if (name == null && areas == null && type == null) {
            return service.search(pageable);
        }
        return service.findAllBySearchCriteria(pageable, type, areas, name);
    }

    @Operation(summary = "Get authorities from service id", tags = "Georegulation (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    @GetMapping("/{id}/authorities")
    @PreAuthorize("hasAuthority('service-user') and hasPermission(#id, 'SERVICE_OPERATOR,SERVICE_ADMIN')")
    public List<MaritimeServiceAuthorityDto> getAuthorities(@PathVariable UUID id) {
        return service.getAuthorities(id);
    }
}
