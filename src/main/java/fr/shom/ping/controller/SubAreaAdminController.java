package fr.shom.ping.controller;

import fr.shom.ping.model.dto.LocalityCreateDto;
import fr.shom.ping.model.dto.SubAreaCreateDto;
import fr.shom.ping.model.dto.SubAreaDetailsDto;
import fr.shom.ping.model.dto.SubAreaSearchDto;
import fr.shom.ping.service.SubAreaService;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.security.Principal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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


import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/admin-api/sub-areas")
@RequiredArgsConstructor
@Tag(name = "Sub Area (Admin)", description = "Sub Area Admin API")
@PreAuthorize("hasAuthority('functional-admin') or hasAuthority('service-user')")
public class SubAreaAdminController {
    private final SubAreaService service;

    @GetMapping(value = "/{id}", consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @Operation(summary = "Get sub area", tags = "Sub Area (Admin)")
    public SubAreaDetailsDto findById(@PathVariable UUID id) {
        return service.findOne(id);
    }

    @PostMapping(consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @Operation(summary = "Create sub area", tags = "Sub Area (Admin)")
    @PreAuthorize("hasAuthority('functional-admin') or hasAuthority('service-user')")
    public SubAreaDetailsDto create(@Validated @RequestBody SubAreaCreateDto dto,
        @Parameter(hidden = true) Principal principal) {
        return service.create(dto, principal.getName());
    }

    @PutMapping(value = "/{id}", consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @Operation(summary = "Update sub area", tags = "Sub Area (Admin)")
    @PreAuthorize("hasAuthority('functional-admin') or hasAuthority('service-user')")
    public SubAreaDetailsDto update(@PathVariable UUID id,
        @Validated @RequestBody SubAreaCreateDto dto,
        @Parameter(hidden = true) Principal principal) {
        return service.update(id, dto, principal.getName());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('functional-admin') or hasAuthority('service-user')")
    @ResponseStatus(NO_CONTENT)
    public void delete(@PathVariable UUID id,
        @Parameter(hidden = true) Principal principal) {
        service.delete(id, principal.getName());
    }

    @GetMapping("/search")
    @Operation(summary = "Get the list of sub areas by page", tags = "Sub Area (Admin)")
    @PreAuthorize("hasAuthority('functional-admin') or hasAuthority('service-user')")
    public Page<SubAreaSearchDto> search(Pageable pageable, @RequestParam(required = false) @NoHtml String technicalIdentifier,
        @RequestParam(required = false) @NoHtml String name, @RequestParam(required = false) @NoHtml String serieName,
        @RequestParam(required = false) @NoHtml String parentName) {
        if (technicalIdentifier == null && name == null && serieName == null && parentName == null) {
            return service.search(pageable);
        }
        return service.findAllBySearchCriteria(pageable, technicalIdentifier, name, serieName, parentName);
    }

    @PostMapping(value = "/{id}/localities")
    public SubAreaDetailsDto addLocality(@PathVariable UUID id, @Valid @RequestBody LocalityCreateDto localityCreateDto) {
        return service.addLocality(id, localityCreateDto);
    }

    @GetMapping
    @Operation(summary = "Get the list of sub areas of an area", tags = "Sub Area")
    public List<SubAreaDetailsDto> findAllByAreaId(@RequestParam UUID areaId) {
        return service.findAllByAreaId(areaId);
    }
}
