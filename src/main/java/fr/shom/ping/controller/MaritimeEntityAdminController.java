package fr.shom.ping.controller;

import fr.shom.ping.model.dto.MaritimeEntityCreateDto;
import fr.shom.ping.model.dto.MaritimeEntityDto;
import fr.shom.ping.model.dto.MaritimeEntitySearchResultDto;
import fr.shom.ping.model.dto.MaritimeEntityToDisplayOnMapDto;
import fr.shom.ping.model.enums.MaritimeEntityType;
import fr.shom.ping.service.MaritimeEntityService;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/admin-api/maritime-entities")
@RequiredArgsConstructor
@Tag(name = "Maritime Entity (Admin)", description = "Maritime Entity Admin API")
public class MaritimeEntityAdminController {
    private final MaritimeEntityService service;

    @PostMapping(consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @Operation(summary = "Create Maritime Entity", tags = "Maritime Entity (Admin)")
    @PreAuthorize("hasAuthority('functional-admin')")
    public MaritimeEntityDto create(@Validated @RequestBody MaritimeEntityCreateDto dto) {
        return this.service.create(dto);
    }

    @PutMapping(value = "/{id}", consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @Operation(summary = "Update Maritime Entity", tags = "Maritime Entity (Admin)")
    @PreAuthorize("hasAuthority('functional-admin')")
    public MaritimeEntityDto update(@PathVariable UUID id, @Validated @RequestBody MaritimeEntityCreateDto dto) {
        return this.service.update(id, dto);
    }

    @GetMapping("/search")
    @Operation(summary = "Get the list of maritime entities by page", tags = "Maritime Entity")
    @PreAuthorize("hasAuthority('functional-admin') or hasAuthority('service-user')")
    public Page<MaritimeEntitySearchResultDto> search(Pageable pageable, @RequestParam(required = false) @NoHtml String name,
        @RequestParam(required = false) List<MaritimeEntityType> type, @RequestParam(required = false) List<UUID> areas,
        @RequestParam(required = false) UUID mainService,
        @RequestParam(required = false) List<UUID> additionalServices) {
        if (name == null && areas == null && type == null && mainService == null && additionalServices == null) {
            return service.search(pageable);
        }
        return service.findAllBySearchCriteria(pageable, type, areas, name, mainService, additionalServices);
    }

    @GetMapping(value = "/available-entities-to-link")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Get available maritime entities to link to warning(s)", tags = "Maritime Entity")
    @Parameter(name = "Authorization", description = "Access Token", required = true, 
        example = "Bearer access_token")
    public List<MaritimeEntityDto> getAvailableEntitiesToLinkFromSerie(
        @RequestParam UUID serieId,
        @Parameter(hidden = true) Principal principal) {
        return service.getAvailableEntitiesToLinkFromSerie(serieId, principal.getName());
    }

    @GetMapping
    @Operation(summary = "Get maritime entities by ids", tags = "Maritime Entity")
    @PreAuthorize("hasAuthority('service-user')")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    public List<MaritimeEntityDto> findAllByIds(@RequestParam List<UUID> entityIds) {
        return service.findAllByIds(entityIds);
    }

    @GetMapping("/entities-managed-or-observed")
    @Operation(summary = "Get maritime entities managed or observed by service to display on map view", tags = "Maritime Entity")
    @PreAuthorize("hasAuthority('service-user')")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    public List<MaritimeEntityToDisplayOnMapDto> getAllEntitiesManagedOrObservedByServiceToDisplayOnMapView(Principal principal) {
        return service.getAllEntitiesManagedOrObservedByServiceToDisplayOnMapView(principal.getName());
    }
}
