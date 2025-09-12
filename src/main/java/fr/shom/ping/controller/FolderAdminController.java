package fr.shom.ping.controller;

import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.dto.FolderAttachmentSaveDto;
import fr.shom.ping.model.dto.FolderAvailableForNotionDto;
import fr.shom.ping.model.dto.FolderDto;
import fr.shom.ping.model.dto.FolderSaveDto;
import fr.shom.ping.model.dto.FolderTableDto;
import fr.shom.ping.model.dto.MaritimeServiceAvailableToFollowFolderDto;
import fr.shom.ping.model.dto.ReferencesByFolderDto;
import fr.shom.ping.model.enums.NotionType;
import fr.shom.ping.model.predicate.FolderSearchCriteria;
import fr.shom.ping.service.FolderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/admin-api/folders")
@RequiredArgsConstructor
@Tag(name = "Folder (Service User)", description = "Folder API")
public class FolderAdminController {

    private final FolderService service;

    @GetMapping
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Get paginated folders by search criteria (Table Public View)", tags = "Folder")
    @Parameter(name = "Authorization", description = "Access Token", required = true,
        example = "Bearer access_token")
    public ResponseEntity<PagedModel<EntityModel<FolderTableDto>>> getAllBySearchCriteria(
        @Valid FolderSearchCriteria searchCriteria,
        Pageable pageable,
        PagedResourcesAssembler<FolderTableDto> pagedResourcesAssembler,
        @Parameter(hidden = true) Principal principal) {
        Page<FolderTableDto> folders = service.findAllBySearchCriteria(searchCriteria, pageable, principal.getName());
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(folders));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Get a folder based on an id", tags = "Folder")
    @Parameter(name = "Authorization", description = "Access Token", required = true,
        example = "Bearer access_token")
    public FolderDto getById(
        @PathVariable UUID id,
        @Parameter(hidden = true) Principal principal) {
        return service.findById(id, principal.getName());
    }

    @GetMapping("/{id}/details")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Get a folder based on an id", tags = "Folder")
    @Parameter(name = "Authorization", description = "Access Token", required = true,
        example = "Bearer access_token")
    public FolderSaveDto getFolderDetailById(
        @PathVariable UUID id,
        @Parameter(hidden = true) Principal principal) {
        return service.findFolderDetailById(id, principal.getName());
    }

    @GetMapping("/references")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Get a folder based on an id", tags = "Folder")
    @Parameter(name = "Authorization", description = "Access Token", required = true,
        example = "Bearer access_token")
    public Set<ReferencesByFolderDto> getAllReferencesByFolderIds(
        @RequestParam List<UUID> folderIds) {
        return service.findAllReferencesByFolderIds(folderIds);
    }

    @PostMapping
    @SuppressWarnings("squid:S4834")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "create a folder", tags = "Folder")
    @Parameter(name = "Authorization", description = "Access Token", required = true,
        example = "Bearer access_token")
    public UUID createFolder(@RequestBody @Valid FolderSaveDto dto, @Parameter(hidden = true) Principal principal) {
        return service.create(dto, principal.getName());
    }

    @PutMapping("/{id}")
    @SuppressWarnings("squid:S4834")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "update a folder", tags = "Folder")
    @Parameter(name = "Authorization", description = "Access Token", required = true,
        example = "Bearer access_token")
    public UUID updateFolder(@PathVariable UUID id, @RequestBody @Valid FolderSaveDto dto, @Parameter(hidden = true) Principal principal) {
        return service.update(id, dto, principal.getName());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('service-user')")
    @ResponseStatus(NO_CONTENT)
    public void delete(@PathVariable UUID id,
        @Parameter(hidden = true) Principal principal) {
        service.delete(id, principal.getName());
    }

    @GetMapping("/available-by-notion")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Get available folders to add notion to", tags = "Folder")
    @Parameter(name = "Authorization", description = "Access Token", required = true,
        example = "Bearer access_token")
    public List<FolderAvailableForNotionDto> getAvailableFoldersToAddNotionTo(
        @RequestParam UUID notionId,
        @RequestParam NotionType type,
        @Parameter(hidden = true) Principal principal) {
        return service.findAvailableFoldersToAddNotionTo(notionId, type, principal.getName());
    }

    @PostMapping("/{id}/notions")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Add notion to folder", tags = "Folder (Service User)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,
        example = "Bearer access_token")
    public void addNotionToFolder(
        @PathVariable UUID id,
        @RequestParam UUID notionId,
        @RequestParam NotionType type,
        @Parameter(hidden = true) Principal principal) {
        service.addNotionToFolder(id, notionId, type, principal.getName());
    }

    @DeleteMapping("/{id}/notions/{notionId}")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Delete notion from folder", tags = "Folder (Service User)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,
        example = "Bearer access_token")
    @ResponseStatus(NO_CONTENT)
    public void removeNotionFromFolder(
        @PathVariable UUID id,
        @PathVariable UUID notionId,
        @Parameter(hidden = true) Principal principal) {
        service.removeNotionFromFolder(id, notionId, principal.getName());
    }

    @PostMapping("/{id}/attachment")
    @SuppressWarnings("squid:S4834")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Add attachment to folder", tags = "Folder")
    @Parameter(name = "Authorization", description = "Access Token", required = true,
        example = "Bearer access_token")
    public UUID addAttachmentToFolder(
        @PathVariable UUID id,
        @RequestBody @Valid FolderAttachmentSaveDto folderAttachmentSaveDto,
        @Parameter(hidden = true) Principal principal) {
        return service.addAttachmentToFolder(id, folderAttachmentSaveDto, principal.getName());
    }

    @PutMapping("/attachment/{folderAttachmentId}")
    @SuppressWarnings("squid:S4834")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Update folder attachment", tags = "Folder")
    @Parameter(name = "Authorization", description = "Access Token", required = true,
        example = "Bearer access_token")
    public UUID updateFolderAttachment(
        @PathVariable UUID folderAttachmentId,
        @RequestBody @Valid FolderAttachmentSaveDto folderAttachmentSaveDto,
        @Parameter(hidden = true) Principal principal) {
        return service.updateFolderAttachment(folderAttachmentId, folderAttachmentSaveDto, principal.getName());
    }

    @DeleteMapping("/attachment/{folderAttachmentId}")
    @SuppressWarnings("squid:S4834")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Delete attachmentFromFolder", tags = "Folder")
    @Parameter(name = "Authorization", description = "Access Token", required = true,
        example = "Bearer access_token")
    @ResponseStatus(NO_CONTENT)
    public void deleteAttachmentFromFolder(
        @PathVariable UUID folderAttachmentId,
        @Parameter(hidden = true) Principal principal) {
        service.deleteAttachmentFromFolder(folderAttachmentId, principal.getName());
    }

    @PostMapping("/{id}/clone")
    @SuppressWarnings("squid:S4834")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Clone a folder", tags = "Folder")
    @Parameter(name = "Authorization", description = "Access Token", required = true,
        example = "Bearer access_token")
    public UUID clone(@PathVariable UUID id, @Parameter(hidden = true) Principal principal) throws ServiceException {
        return service.clone(id, principal.getName());
    }

    @PostMapping("/{id}/notify")
    @SuppressWarnings("squid:S4834")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Notify a folder", tags = "Folder")
    @Parameter(name = "Authorization", description = "Access Token", required = true,
        example = "Bearer access_token")
    public void notify(
        @PathVariable UUID id,
        @RequestBody Set<UUID> serviceIds,
        @Parameter(hidden = true) Principal principal) {
        service.notify(id, serviceIds, principal.getName());
    }

    @GetMapping("/available-maritime-services")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Get available maritime services to follow folder", tags = "Folder")
    @Parameter(name = "Authorization", description = "Access Token", required = true,
        example = "Bearer access_token")
    public List<MaritimeServiceAvailableToFollowFolderDto> getAvailableMaritimeServicesToFollowFolder(
        @RequestParam UUID folderId,
        @Parameter(hidden = true) Principal principal) {
        return service.findAvailableMaritimeServicesToFollowFolder(folderId, principal.getName());
    }

    @PostMapping("/{id}/follow")
    @SuppressWarnings("squid:S4834")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Follow a folder", tags = "Folder")
    @Parameter(name = "Authorization", description = "Access Token", required = true,
        example = "Bearer access_token")
    public void follow(
        @PathVariable UUID id,
        @RequestBody Set<UUID> serviceIds,
        @Parameter(hidden = true) Principal principal) {
        service.follow(id, serviceIds, principal.getName());
    }

    @PostMapping("/{id}/un-follow")
    @SuppressWarnings("squid:S4834")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "unFollow a folder", tags = "Folder")
    @Parameter(name = "Authorization", description = "Access Token", required = true,
        example = "Bearer access_token")
    public void unFollow(
        @PathVariable UUID id,
        @RequestParam UUID serviceId,
        @Parameter(hidden = true) Principal principal) {
        service.unFollow(id, serviceId, principal.getName());
    }
}
