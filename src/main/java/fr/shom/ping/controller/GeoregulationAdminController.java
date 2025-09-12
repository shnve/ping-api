package fr.shom.ping.controller;

import fr.shom.ping.model.dto.GeoregulationAdminTableDto;
import fr.shom.ping.model.dto.GeoregulationCreateDto;
import fr.shom.ping.model.dto.GeoregulationEditDto;
import fr.shom.ping.model.dto.GeoregulationModalDetailsDto;
import fr.shom.ping.model.dto.GeoregulationSelectDto;
import fr.shom.ping.model.dto.GeoregulationSummaryUpdateDto;
import fr.shom.ping.model.dto.GeoregulationToDisplayOnCreationMapDto;
import fr.shom.ping.model.dto.GeoregulationUpdateDto;
import fr.shom.ping.model.dto.MaritimeEntityConcernedDto;
import fr.shom.ping.model.dto.MaritimeServiceInfosourceRecipientDto;
import fr.shom.ping.model.dto.MaritimeServiceSelectDto;
import fr.shom.ping.model.enums.GeoregulationType;
import fr.shom.ping.model.predicate.GeoregulationAdminSearchCriteria;
import fr.shom.ping.service.GeoregulationAdminService;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.security.Principal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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


import static org.springframework.http.HttpStatus.NO_CONTENT;

@Tag(name = "Georegulation (Admin)", description = "Georegulation Admin API")
@RestController
@RequestMapping("/admin-api/georegulations")
@RequiredArgsConstructor
public class GeoregulationAdminController {

    private final GeoregulationAdminService service;

    @SuppressWarnings("squid:S4834")
    @Operation(summary = "Create georegulation", tags = "Georegulation (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    @PostMapping
    @PreAuthorize("hasAuthority('service-user') and hasPermission(#dto.serviceId, 'SERVICE_OPERATOR,SERVICE_ADMIN')")
    public UUID create(@Valid @RequestBody GeoregulationCreateDto dto, @Parameter(hidden = true) Principal principal) {
        return service.create(dto, principal.getName());
    }

    @SuppressWarnings("squid:S4834")
    @Operation(summary = "Get geometries for given georegulation type", tags = "Georegulation (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    @GetMapping(value = "/for-creation-map")
    @PreAuthorize("hasAuthority('service-user') and hasPermission(#issuingServiceId, 'SERVICE_OPERATOR,SERVICE_ADMIN')")
    public List<GeoregulationToDisplayOnCreationMapDto> findAllToDisplayInCreationMap(
        @RequestParam GeoregulationType georegulationType,
        @RequestParam UUID issuingServiceId) {
        return service.findAllToDisplayOnCreationMap(georegulationType, issuingServiceId);
    }

    @SuppressWarnings("squid:S4834")
    @Operation(summary = "Get georegulations by search criteria", tags = "Georegulation (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    @GetMapping
    @PreAuthorize("hasAuthority('service-user') and hasPermission(#searchCriteria.issuingServiceId, '*')")
    public ResponseEntity<PagedModel<EntityModel<GeoregulationAdminTableDto>>> getAllBySearchCriteria(
        @Valid GeoregulationAdminSearchCriteria searchCriteria,
        Pageable pageable,
        @Parameter(hidden = true) PagedResourcesAssembler<GeoregulationAdminTableDto> pagedResourcesAssembler) {
        Page<GeoregulationAdminTableDto> allBySearchCriteria = service.findAllBySearchCriteria(searchCriteria, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(allBySearchCriteria));
    }

    @Operation(summary = "Get authorities to select for current user", tags = "Georegulation (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    @GetMapping("/authorities-to-select")
    @PreAuthorize("hasAuthority('service-user')")
    public List<MaritimeServiceSelectDto> getAuthoritiesToSelect(Principal principal) {
        return service.getAuthoritiesToSelect(principal.getName());
    }

    @Operation(summary = "Get authorities to filter for issuing service", tags = "Georegulation (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    @GetMapping("/authorities-to-filter")
    @PreAuthorize("hasAuthority('service-user') and hasPermission(#issuingServiceId, '*')")
    public List<MaritimeServiceSelectDto> getAuthoritiesToFilter(@RequestParam UUID issuingServiceId) {
        return service.getAuthoritiesWithAreasInIssuingServiceAreas(issuingServiceId);
    }

    @SuppressWarnings("squid:S4834")
    @GetMapping("/entities-to-filter")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Get entities to filter", tags = "Georegulation (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    public List<MaritimeEntityConcernedDto> getEntitiesToFilter(Principal principal) {
        return service.getAllGeoregulationEntities(principal.getName());
    }


    @SuppressWarnings("squid:S4834")
    @Operation(summary = "Get georegulation (modal details)", tags = "Georegulation (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    @GetMapping("/{id}/modal-details")
    @PreAuthorize("hasAuthority('functional-admin') or hasAuthority('service-user')")
    public GeoregulationModalDetailsDto getModalDetails(@PathVariable UUID id, @Parameter(hidden = true) Principal principal) {
        return service.getModalDetails(id, principal.getName());
    }

    @Operation(summary = "Publish georegulation", tags = "Georegulation (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('service-user')")
    public void publish(@PathVariable UUID id, @Parameter(hidden = true) Principal principal) {
        service.publish(id, principal.getName());
    }

    @Operation(summary = "Repeal georegulation", tags = "Georegulation (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    @PutMapping("/{id}/repeal")
    @PreAuthorize("hasAuthority('service-user')")
    public void repeal(@PathVariable UUID id, @Parameter(hidden = true) Principal principal) {
        service.repeal(id, principal.getName());
    }

    @SuppressWarnings("squid:S4834")
    @Operation(summary = "Get recipients to send georegulation", tags = "Georegulation (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    @GetMapping("/{id}/recipients")
    @PreAuthorize("hasAuthority('service-user')")
    public List<MaritimeServiceInfosourceRecipientDto> getRecipients(@PathVariable UUID id, @Parameter(hidden = true) Principal principal) {
        return service.getRecipients(id, principal.getName());
    }

    @SuppressWarnings("squid:S4834")
    @Operation(summary = "Send georegulation", tags = "Georegulation (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    @PutMapping("/{id}/send")
    @PreAuthorize("hasAuthority('service-user')")
    public void send(
        @PathVariable UUID id,
        @RequestBody Set<UUID> recipientIds,
        @Parameter(hidden = true) Principal principal) {
        service.send(id, recipientIds, principal.getName());
    }

    @SuppressWarnings("squid:S4834")
    @Operation(summary = "Create georegulation", tags = "Georegulation (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    @PutMapping
    @PreAuthorize("hasAuthority('service-user') and hasPermission(#dto.serviceId, 'SERVICE_OPERATOR,SERVICE_ADMIN')")
    public void update(@Valid @RequestBody GeoregulationUpdateDto dto, @Parameter(hidden = true) Principal principal) {
        service.update(dto, principal.getName());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get the georegulation by ID", tags = "Georegulation (Admin)")
    @PreAuthorize("hasAuthority('service-user')")
    public GeoregulationEditDto findGeoregulationById(@PathVariable UUID id,
        @Parameter(hidden = true) Principal principal) {
        return service.findById(id, principal.getName());
    }

    @Operation(summary = "Find available georegulations to link to warning", tags = "Georegulation (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    @GetMapping("/available-georegulations-to-link")
    @PreAuthorize("hasAuthority('service-user')")
    public List<GeoregulationSelectDto> findAvailableToLinkToWarning(
        @RequestParam UUID serieId,
        @Parameter(hidden = true) Principal principal) {
        return service.findAvailableToLinkToWarning(serieId, principal.getName());
    }

    @SuppressWarnings("squid:S4834")
    @Operation(summary = "Update georegulation summary", tags = "Georegulation (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    @PutMapping("/{id}/summary-and-restriction-categories")
    @PreAuthorize("hasAuthority('service-user')")
    public void updateSummary(@PathVariable UUID id,
        @Validated @RequestBody GeoregulationSummaryUpdateDto dto,
        @Parameter(hidden = true) Principal principal) {
        service.updateSummary(id, dto, principal.getName());
    }

    @SuppressWarnings("squid:S4834")
    @Operation(summary = "Delete georegulation", tags = "Georegulation (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('service-user')")
    @ResponseStatus(NO_CONTENT)
    public void delete(
        @PathVariable UUID id,
        @Parameter(hidden = true) Principal principal) {
        service.delete(id, principal.getName());
    }

    @SuppressWarnings("squid:S4834")
    @Operation(summary = "Clone a georegulation", tags = "Georegulation (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    @PostMapping("/{id}/clone")
    @PreAuthorize("hasAuthority('service-user')")
    public UUID clone(
        @PathVariable UUID id,
        @RequestBody @Valid @NoHtml String clonedGeoregulationNumberText,
        @Parameter(hidden = true) Principal principal) {
        return service.clone(id, clonedGeoregulationNumberText, principal.getName());
    }

    @GetMapping("/available-georegulations-to-link-as-reference")
    @SuppressWarnings("squid:S4834")
    @Operation(summary = "Find available georegulations to link as warning public reference", tags = "Georegulation (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    @PreAuthorize("hasAuthority('service-user')")
    public Set<GeoregulationSelectDto> findAvailableToLinkAsWarningPublicReference(
        @RequestParam UUID warningId,
        @Parameter(hidden = true) Principal principal) {
        return service.findAvailableToLinkAsWarningPublicReference(warningId, principal.getName());
    }
}
