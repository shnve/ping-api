package fr.shom.ping.controller;


import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.Warning;
import fr.shom.ping.model.dto.AttachmentDownloadDto;
import fr.shom.ping.model.dto.FolderConcernedDto;
import fr.shom.ping.model.dto.MaritimeEntityConcernedDto;
import fr.shom.ping.model.dto.WarningAdminDetailDto;
import fr.shom.ping.model.dto.WarningAdminEditDto;
import fr.shom.ping.model.dto.WarningAdminTableDto;
import fr.shom.ping.model.dto.WarningCreateDto;
import fr.shom.ping.model.dto.WarningMaritimeEntityReferenceDto;
import fr.shom.ping.model.dto.WarningReferenceAsPublicReferenceDto;
import fr.shom.ping.model.dto.WarningReferenceDto;
import fr.shom.ping.model.dto.WarningUpdateDto;
import fr.shom.ping.model.dto.WarningUpdatePublishedDto;
import fr.shom.ping.model.enums.WarningReferenceCategoryEnum;
import fr.shom.ping.model.enums.WarningStatus;
import fr.shom.ping.model.predicate.WarningAdminSearchCriteria;
import fr.shom.ping.service.WarningAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

@RestController
@RequestMapping("/admin-api/warnings")
@RequiredArgsConstructor
public class WarningAdminController {

    private final WarningAdminService service;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PagedModel<EntityModel<WarningAdminTableDto>>> getAllBySearchCriteria(
        @Valid WarningAdminSearchCriteria searchCriteria,
        Pageable pageable,
        PagedResourcesAssembler<WarningAdminTableDto> pagedResourcesAssembler) {
        Page<WarningAdminTableDto> warnings = service.findAllBySearchCriteria(searchCriteria, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(warnings));
    }

    @PostMapping
    @PreAuthorize("hasPermission(#warningCreateDto.serviceId, 'SERVICE_ADMIN') or hasPermission(#warningCreateDto.serviceId, 'SERVICE_OPERATOR')")
    public WarningAdminEditDto create(@RequestBody @Valid WarningCreateDto warningCreateDto, Principal principal) {
        return service.create(warningCreateDto, principal.getName());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(#warningUpdateDto.serviceId, 'SERVICE_ADMIN') or hasPermission(#warningUpdateDto.serviceId, 'SERVICE_OPERATOR')")
    public WarningAdminEditDto update(@PathVariable UUID id, @RequestBody @Valid WarningUpdateDto warningUpdateDto,
        Principal principal) {
        return service.update(id, warningUpdateDto, principal.getName());
    }

    @PutMapping("/{id}/published")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    @PreAuthorize("hasPermission(#dto.serviceId, 'SERVICE_ADMIN') or hasPermission(#dto.serviceId, 'SERVICE_OPERATOR')")
    public WarningAdminEditDto updatePublished(
        @PathVariable UUID id,
        @RequestBody @Valid WarningUpdatePublishedDto dto,
        @Parameter(hidden = true) Principal principal) {
        return service.updatePublished(id, dto, principal.getName());
    }

    @GetMapping("/{id}/details-view")
    @PreAuthorize("hasAuthority('service-user')")
    public WarningAdminDetailDto getForDetailsView(
        @PathVariable UUID id,
        @Parameter(hidden = true) Principal principal) {
        return service.findByIdAndMapForDetailsView(id, principal.getName());
    }

    @GetMapping("/cancellable")
    @PreAuthorize("hasAuthority('service-user')")
    public List<WarningReferenceDto> getCancellable(@RequestParam UUID serieId) {
        return service.findCancellable(serieId);
    }

    @GetMapping("/replaceable")
    @PreAuthorize("hasAuthority('service-user')")
    public List<WarningReferenceDto> getReplaceable(@RequestParam UUID serieId, Principal principal) {
        return service.findReplaceable(serieId, principal.getName());
    }

    @GetMapping("/referenceable")
    @PreAuthorize("hasAuthority('service-user')")
    public List<WarningReferenceDto> getReferenceable() {
        return service.findReferenceable();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('service-user')")
    public WarningAdminEditDto getOne(@PathVariable UUID id) {
        return service.findOne(id);
    }

    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('service-user')")
    public void publish(@PathVariable UUID id, Principal principal) throws ServiceException {
        service.publish(id, principal.getName());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('service-user')")
    @ResponseStatus(NO_CONTENT)
    public void delete(@PathVariable UUID id, Principal principal) {
        service.delete(id, principal.getName());
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('service-user')")
    public UUID cancel(@PathVariable UUID id, Principal principal) throws ServiceException {
        return service.cancel(id, principal.getName());
    }

    @PutMapping("/{id}/clone")
    @PreAuthorize("hasAuthority('service-user')")
    public UUID clone(@PathVariable UUID id,
        @RequestParam UUID serieId,
        @RequestParam(required = false) WarningReferenceCategoryEnum warningReferenceCategoryEnum,
        @RequestParam(required = false) WarningStatus warningStatus,
        Principal principal) {
        return service.clone(id, serieId, warningReferenceCategoryEnum, warningStatus, principal.getName());
    }

    @GetMapping("/{id}/telex")
    @PreAuthorize("hasAuthority('service-user')")
    public String generateTelex(@PathVariable UUID id) {
        return service.generateTelex(id);
    }

    @GetMapping("/{id}/navtex")
    @PreAuthorize("hasAuthority('service-user')")
    public String generateNavtex(@PathVariable UUID id) {
        return service.generateNavtex(id);
    }

    @GetMapping("/entities-to-filter")
    @SuppressWarnings("squid:S4834")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Get entities to filter", tags = "Warning (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    public List<MaritimeEntityConcernedDto> getEntitiesToFilter(Principal principal) {
        return service.getAllWarningEntities(principal.getName());
    }

    @GetMapping("/entities-managed-or-observed-to-filter")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Get entities to filter", tags = "Warning (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    public List<MaritimeEntityConcernedDto> getEntitiesManagedOrObservedByService(Principal principal) {
        return service.getAllEntitiesManagedOrObservedByService(principal.getName());
    }

    @GetMapping("/folders-to-filter")
    @SuppressWarnings("squid:S4834")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Get folders to filter", tags = "Warning (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    public List<FolderConcernedDto> getFoldersToFilter() {
        return service.getAllWarningFolders();
    }

    @GetMapping("/linked-entities")
    @SuppressWarnings("squid:S4834")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Get linked entities to filter", tags = "Warning (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    public List<MaritimeEntityConcernedDto> getLinkedEntities(
        @RequestParam UUID serieId,
        @RequestParam boolean isFromAvurnavNoticeTable,
        Principal principal) {
        return service.getLinkedEntities(principal.getName(), serieId, isFromAvurnavNoticeTable);
    }

    @PutMapping("/{id}/set-reviewed")
    @SuppressWarnings("squid:S4834")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Set force_to_review to false", tags = "Warning (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    public void setReviewed(@PathVariable UUID id,
        Principal principal) {
        service.setReviewed(id, principal.getName());
    }

    @GetMapping("/available-attachments-to-link-as-reference")
    @SuppressWarnings("squid:S4834")
    @Operation(summary = "Find available attachments to link as warning public reference", tags = "Warning (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    @PreAuthorize("hasAuthority('service-user')")
    public Set<AttachmentDownloadDto> findAvailableAttachmentsToLinkAsWarningPublicReference(
        @RequestParam UUID warningId,
        @Parameter(hidden = true) Principal principal) {
        return service.findAvailableAttachmentsToLinkAsWarningPublicReference(warningId, principal.getName());
    }

    @GetMapping("/available-entities-references-to-link-as-reference")
    @SuppressWarnings("squid:S4834")
    @Operation(summary = "Find available entity references to link as warning public reference", tags = "Warning (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    @PreAuthorize("hasAuthority('service-user')")
    public Set<WarningMaritimeEntityReferenceDto> findAvailableMaritimeEntitiesReferencesToLinkAsWarningPublicReference(
        @RequestParam UUID warningId,
        @Parameter(hidden = true) Principal principal) {
        return service.findAvailableMaritimeEntitiesReferencesToLinkAsWarningPublicReference(warningId, principal.getName());
    }

    @GetMapping("/available-warnings-references-to-link-as-reference")
    @SuppressWarnings("squid:S4834")
    @Operation(summary = "Find available warning references to link as warning public reference", tags = "Warning (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    @PreAuthorize("hasAuthority('service-user')")
    public Set<WarningReferenceAsPublicReferenceDto> findAvailableWarningsReferencesToLinkAsWarningPublicReference(
        @RequestParam UUID warningId,
        @Parameter(hidden = true) Principal principal) {
        return service.findAvailableWarningsReferencesToLinkAsWarningPublicReference(warningId, principal.getName());
    }

    @GetMapping("/get-warning-ids-which-can-be-cancelled")
    @SuppressWarnings("squid:S4834")
    @Operation(summary = "Get ids of warnings which can be cancelled by publishing of warning referenced by warningId parameter", tags = "Warning (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    @PreAuthorize("hasAuthority('service-user')")
    public Set<UUID> getWarningIdsWhichCanBeCancelled(
        @RequestParam UUID warningId,
        @Parameter(hidden = true) Principal principal) {
        return service.getWarningIdsWhichCanBeCancelled(warningId, principal.getName());
    }
}
