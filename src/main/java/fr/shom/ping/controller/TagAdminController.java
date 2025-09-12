package fr.shom.ping.controller;

import fr.shom.ping.model.dto.TagCreateDto;
import fr.shom.ping.model.dto.TagDto;
import fr.shom.ping.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/admin-api/maritime-service-tags")
@RequiredArgsConstructor
@Tag(name = "Service tag (Admin)", description = "Service tag Admin API")
public class TagAdminController {
    private final TagService service;

    @PostMapping(consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @Operation(summary = "Create Service Tag", tags = "Service Tag (Admin)")
    @PreAuthorize("hasAuthority('functional-admin') or " +
        "(hasAuthority('service-user') and (hasPermission(#dto.maritimeServiceId, 'SERVICE_ADMIN,SERVICE_OPERATOR')))")
    @ResponseStatus(CREATED)
    public TagDto create(@Validated @RequestBody TagCreateDto dto) {
        return service.create(dto);
    }

    @PutMapping(value = "/{id}", consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @Operation(summary = "Update Service Tag", tags = "Service Tag (Admin)")
    @PreAuthorize("hasAuthority('functional-admin') or (hasAuthority('service-user') and hasPermission(#dto.maritimeServiceId, 'SERVICE_ADMIN'))")
    public TagDto update(@PathVariable UUID id, @Validated @RequestBody TagCreateDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping(value = "/{id}")
    @Operation(summary = "Remove Service Tag from Maritime Service", tags = "Service Tag (Admin)")
    @PreAuthorize("hasAuthority('functional-admin') or (hasAuthority('service-user') and hasPermission(#maritimeServiceId, 'SERVICE_ADMIN'))")
    @ResponseStatus(NO_CONTENT)
    public void delete(@PathVariable UUID id, @RequestParam UUID maritimeServiceId) {
        service.delete(id);
    }

}
