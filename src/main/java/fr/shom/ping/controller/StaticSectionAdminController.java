package fr.shom.ping.controller;

import fr.shom.ping.model.dto.StaticSectionDto;
import fr.shom.ping.model.dto.StaticSectionUpdateDto;
import fr.shom.ping.service.StaticSectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@RestController
@RequestMapping("/admin-api/static-sections")
@RequiredArgsConstructor
@Tag(name = "Static section admin")
public class StaticSectionAdminController {

    private final StaticSectionService service;

    @GetMapping("/{id}")
    @Operation(summary = "Get the static section by id", tags = "Static section (Admin)")
    @PreAuthorize("hasAuthority('functional-admin')")
    public StaticSectionDto findById(@PathVariable UUID id) {
        return service.findById(id).asDto();
    }

    @PutMapping(value = "/{id}", consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @Operation(summary = "Update static section", tags = "Static section (Admin)")
    @PreAuthorize("hasAuthority('functional-admin')")
    public StaticSectionDto update(@PathVariable UUID id, @Valid @RequestBody StaticSectionUpdateDto dto) {
        return service.update(id, dto);
    }

    @GetMapping("/search")
    @Operation(summary = "Get the list of static section by page", tags = "Static section (Admin)")
    @PreAuthorize("hasAuthority('functional-admin')")
    public Page<StaticSectionDto> search(Pageable pageable) {
        return this.service.search(pageable);
    }
}
