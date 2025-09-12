package fr.shom.ping.controller;

import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.dto.WarningLinkCreateDto;
import fr.shom.ping.model.dto.WarningLinkDto;
import fr.shom.ping.service.WarningLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import java.security.Principal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/admin-api/warning-links")
@RequiredArgsConstructor
@Tag(name = "Warning Link (Admin)", description = "Warning Link Admin API")
public class WarningLinkAdminController {

    private final WarningLinkService service;

    @PostMapping(consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @SuppressWarnings("squid:S4834")
    @Operation(summary = "Create Warning Link", tags = "Warning Link (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    @PreAuthorize("hasAuthority('service-user')")
    @ResponseStatus(CREATED)
    public WarningLinkDto create(
        @Validated @RequestBody WarningLinkCreateDto dto,
        @Parameter(hidden = true) Principal principal) {
        return service.create(dto, principal.getName());
    }

    @PutMapping(value = "/{id}", consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @SuppressWarnings("squid:S4834")
    @Operation(summary = "Update Warning Link", tags = "Warning Link (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    @PreAuthorize("hasAuthority('service-user')")
    public WarningLinkDto update(
        @PathVariable UUID id,
        @Validated @RequestBody WarningLinkCreateDto dto,
        @Parameter(hidden = true) Principal principal) {
        return service.update(id, dto, principal.getName());
    }

    @DeleteMapping(value = "/{id}")
    @SuppressWarnings("squid:S4834")
    @Operation(summary = "Remove Warning Link from Maritime Entity", tags = "Warning Link (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    @PreAuthorize("hasAuthority('service-user')")
    @ResponseStatus(NO_CONTENT)
    public void delete(
        @PathVariable UUID id,
        @Parameter(hidden = true) Principal principal) {
        service.delete(id, principal.getName());
    }
}

