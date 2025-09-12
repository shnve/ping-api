package fr.shom.ping.controller;

import fr.shom.ping.model.Serie;
import fr.shom.ping.model.dto.SerieCreateDto;
import fr.shom.ping.model.dto.SerieDetailsDto;
import fr.shom.ping.model.dto.SerieDto;
import fr.shom.ping.service.SerieService;
import fr.shom.ping.service.WarningAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import java.security.Principal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@RestController
@RequestMapping("/admin-api/series")
@RequiredArgsConstructor
@Tag(name = "Serie (Admin)", description = "Serie Admin API")
public class SerieAdminController {
    private final SerieService service;
    private final WarningAdminService warningAdminService;

    @PostMapping(consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @Operation(summary = "Create serie", tags = "Serie (Admin)")
    @PreAuthorize("hasAuthority('functional-admin')")
    public SerieDetailsDto create(@Validated @RequestBody SerieCreateDto dto) {
        return service.create(dto);
    }

    @PutMapping(value = "/{id}", consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @Operation(summary = "Update serie", tags = "Serie (Admin)")
    @PreAuthorize("hasAuthority('functional-admin')")
    public SerieDetailsDto update(@PathVariable UUID id, @Validated @RequestBody SerieCreateDto dto) {
        return service.update(id, dto);
    }

    @GetMapping("/search")
    @Operation(summary = "Get the list of serie by page", tags = "Serie")
    @PreAuthorize("hasAuthority('functional-admin') or hasAuthority('service-user')")
    public Page<Serie> search(Pageable pageable) {
        return service.search(pageable);
    }

    @GetMapping
    @Operation(summary = "Get all series by service ids", tags = "Serie (Admin)")
    @PreAuthorize("hasAuthority('functional-admin') or (hasAuthority('service-user') and hasPermission(#serviceIds, '*'))")
    public List<Serie> getAllByServices(@RequestParam List<UUID> serviceIds) {
        return service.findAllByServiceIds(serviceIds);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get the serie by ID", tags = "Serie (Admin)")
    @PreAuthorize("hasAuthority('functional-admin') or hasAuthority('service-user')")
    public SerieDetailsDto findById(@PathVariable UUID id) {
        return service.findById(id).asDto();
    }

    @GetMapping("/{id}/isWarningNumberAvailable")
    @Operation(summary = "Check if warningNumber is available for serie and year", tags = "Serie (Admin)")
    @PreAuthorize("hasAuthority('functional-admin') or hasAuthority('service-user')")
    public boolean isWarningNumberAvailable(@PathVariable UUID id, @RequestParam Integer warningNumber, Integer year) {
        return warningAdminService.isWarningNumberAvailable(service.findById(id), warningNumber, year);
    }

    @GetMapping("/{id}/nextAvailableNumber")
    @Operation(summary = "Get the next available number for serie and year", tags = "Serie (Admin)")
    @PreAuthorize("hasAuthority('functional-admin') or hasAuthority('service-user')")
    public Integer nextAvailableNumber(@PathVariable UUID id, Integer year) {
        return warningAdminService.nextAvailableNumber(service.findById(id), year);
    }

    @GetMapping("/by-user")
    @Operation(summary = "Get the available series by service user with admin or operator role", tags = "Serie (Admin)")
    @PreAuthorize("hasAuthority('service-user')")
    public List<SerieDto> findByUser(Principal principal) {
        return service.findByUserWithAdminOrOperatorRole(principal.getName());
    }
}
