package fr.shom.ping.controller;

import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.dto.MaritimeEntityReferenceCreateDto;
import fr.shom.ping.model.dto.MaritimeEntityReferenceDto;
import fr.shom.ping.service.MaritimeEntityReferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/admin-api/maritime-entity-references")
@RequiredArgsConstructor
@Tag(name = "Maritime Entity Reference (Admin)", description = "Maritime Entity Reference Admin API")
public class MaritimeEntityReferenceAdminController {

    private final MaritimeEntityReferenceService service;

    @PostMapping(consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @Operation(summary = "Create Maritime Entity Reference", tags = "Maritime Entity Reference (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    @PreAuthorize("hasAuthority('functional-admin')")
    @ResponseStatus(CREATED)
    public MaritimeEntityReferenceDto create(@Validated @RequestBody MaritimeEntityReferenceCreateDto dto) {
        return service.create(dto);
    }

    @PutMapping(value = "/{id}", consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @Operation(summary = "Update Maritime Entity Reference", tags = "Maritime Entity Reference (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    @PreAuthorize("hasAuthority('functional-admin')")
    public MaritimeEntityReferenceDto update(@PathVariable UUID id, @Validated @RequestBody MaritimeEntityReferenceCreateDto dto) throws ServiceException {
        return service.update(id, dto);
    }

    @DeleteMapping(value = "/{id}")
    @Operation(summary = "Remove Maritime Entity Reference from Maritime Entity", tags = "Maritime Entity Reference (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    @PreAuthorize("hasAuthority('functional-admin')")
    @ResponseStatus(NO_CONTENT)
    public void delete(@PathVariable UUID id) throws ServiceException {
        service.delete(id);
    }
}
