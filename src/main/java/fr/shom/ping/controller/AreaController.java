package fr.shom.ping.controller;

import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.model.Area;
import fr.shom.ping.model.dto.AreaDto;
import fr.shom.ping.model.dto.AreaServiceDto;
import fr.shom.ping.repository.AreaRepository;
import fr.shom.ping.service.MaritimeServiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/areas")
@RequiredArgsConstructor
@Tag(name = "Area", description = "Area API")
public class AreaController {

    private final AreaRepository repository;
    private final MaritimeServiceService maritimeServiceService;

    @GetMapping
    @Operation(summary = "Get the list of areas", tags = "Area")
    public List<Area> get() {
        return repository.findAll(Sort.by("name"));
    }

    @GetMapping("/{id}")
    public AreaDto findById(@PathVariable UUID id) {
        return repository.findById(id).map(Area::asDto).orElseThrow(NotFoundException::new);
    }

    @GetMapping("/services")
    @Operation(summary = "Get the list of areas for a list of services", tags = "Area")
    public List<AreaServiceDto> getByServiceIds(@RequestParam List<UUID> serviceIds) {
        return maritimeServiceService.findAreasByServiceIdsIn(serviceIds);
    }
}
