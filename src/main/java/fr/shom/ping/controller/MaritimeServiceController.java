package fr.shom.ping.controller;

import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.dto.MaritimeServiceDto;
import fr.shom.ping.repository.MaritimeServiceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/maritime-services")
@RequiredArgsConstructor
@Tag(name = "Maritime Service", description = "Maritime Service API")
public class MaritimeServiceController {

    private final MaritimeServiceRepository repository;

    @GetMapping
    @Operation(summary = "Get the list of maritime services", tags = "Maritime Service")
    public List<MaritimeServiceDto> get() {
        return repository.findAll().stream().map(MaritimeService::asDto).collect(Collectors.toList());
    }
}
