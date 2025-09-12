package fr.shom.ping.controller;

import fr.shom.ping.model.Area;
import fr.shom.ping.model.dto.AreaCreateDto;
import fr.shom.ping.model.dto.AreaDto;
import fr.shom.ping.service.AreaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;


@RestController
@RequestMapping("/admin-api/areas")
@RequiredArgsConstructor
@Tag(name = "Area (Admin)", description = "Area Admin API")
public class AreaAdminController {
    private final AreaService service;

    @PostMapping(consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @Operation(summary = "Create area", tags = "Area (Admin)")
    @PreAuthorize("hasAuthority('functional-admin')")
    public AreaDto create(@Validated @RequestBody AreaCreateDto dto) {
        return this.service.create(dto);
    }

    @PutMapping(value = "/{id}", consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @Operation(summary = "Update area", tags = "Area (Admin)")
    @PreAuthorize("hasAuthority('functional-admin')")
    public AreaDto update(@PathVariable UUID id, @Validated @RequestBody AreaCreateDto dto) {
        return this.service.update(id, dto);
    }

    @GetMapping("/search")
    @Operation(summary = "Get the list of areas by page", tags = "Area")
    @PreAuthorize("hasAuthority('functional-admin') or hasAuthority('service-user')")
    public Page<Area> search(Pageable pageable) {
        return this.service.search(pageable);
    }
}
