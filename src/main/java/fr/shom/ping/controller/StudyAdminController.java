package fr.shom.ping.controller;

import fr.shom.ping.model.dto.StudyCreateDto;
import fr.shom.ping.model.dto.StudyDto;
import fr.shom.ping.model.enums.StudyStatus;
import fr.shom.ping.service.StudyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import java.security.Principal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/admin-api/studies")
@RequiredArgsConstructor
@Tag(name = "Study (Service User)", description = "Study API")
public class StudyAdminController {
    private final StudyService service;

    @PostMapping(consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @Operation(summary = "Create study", tags = "Study (Admin)")
    @PreAuthorize("hasAuthority('service-user')")
    @ResponseStatus(CREATED)
    public StudyDto create(
        @Validated @RequestBody StudyCreateDto dto,
        @Parameter(hidden = true) Principal principal) {
        return service.create(dto, principal.getName());
    }

    @PutMapping(value = "/{id}", consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @Operation(summary = "Update study", tags = "Study (Admin)")
    @PreAuthorize("hasAuthority('service-user')")
    @Parameter(name = "Authorization", description = "Access Token", required = true,
        example = "Bearer access_token")
    public StudyDto update(
        @PathVariable UUID id,
        @Validated @RequestBody StudyCreateDto dto,
        @Parameter(hidden = true) Principal principal) {
        return service.update(id, dto, principal.getName());
    }

    @PutMapping(value = "/{id}/status")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Update a study status", tags = "Study")
    @Parameter(name = "Authorization", description = "Access Token", required = true,
        example = "Bearer access_token")
    @ResponseStatus(NO_CONTENT)
    public void updateStatus(
        @PathVariable UUID id,
        @RequestParam StudyStatus status,
        @Parameter(hidden = true) Principal principal) {
        service.updateStatus(id, status, principal.getName());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Delete study", tags = "Study (Service User)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,
        example = "Bearer access_token")
    @ResponseStatus(NO_CONTENT)
    public void delete(@PathVariable UUID id, @Parameter(hidden = true) Principal principal) {
        service.delete(id, principal.getName());
    }
}
