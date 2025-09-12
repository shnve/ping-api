package fr.shom.ping.controller;

import fr.shom.ping.exception.BindingException;
import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.dto.StaticLogoDto;
import fr.shom.ping.service.StaticLogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/admin-api/static-logos")
@RequiredArgsConstructor
@Tag(name = "Static logo admin")
public class StaticLogoAdminController {
    private final StaticLogoService service;

    @GetMapping("/{id}")
    @Operation(summary = "Get the static logo by id", tags = "Static logo (Admin)")
    @PreAuthorize("hasAuthority('functional-admin')")
    public StaticLogoDto findById(@PathVariable UUID id) {
        return service.findById(id).asDto();
    }

    @PostMapping(consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @Operation(summary = "Create static logo", tags = "Static logo (Admin)")
    @PreAuthorize("hasAuthority('functional-admin')")
    public StaticLogoDto create(@Validated @RequestBody StaticLogoDto dto, BindingResult bindingResult) {
        try {
            return service.create(dto, bindingResult);
        } catch (ServiceException exception) {
            throw new BindingException(bindingResult.getAllErrors());
        }
    }

    @PutMapping(value = "/{id}", consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @Operation(summary = "Update static logo", tags = "Static logo (Admin)")
    @PreAuthorize("hasAuthority('functional-admin')")
    public StaticLogoDto update(@PathVariable UUID id, @Valid @RequestBody StaticLogoDto dto, BindingResult bindingResult) throws ServiceException {
        try {
            return service.update(id, dto, bindingResult);
        } catch (ServiceException exception) {
            throw new BindingException(bindingResult.getAllErrors());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('functional-admin')")
    @ResponseStatus(NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }


    @GetMapping("/search")
    @Operation(summary = "Get the list of static logo by page", tags = "Static logo (Admin)")
    @PreAuthorize("hasAuthority('functional-admin')")
    public Page<StaticLogoDto> search(Pageable pageable) {
        return this.service.search(pageable);
    }
}
