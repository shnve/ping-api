package fr.shom.ping.controller;

import fr.shom.ping.repository.MaritimeServiceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/maritime-services")
@RequiredArgsConstructor
@Tag(name = "Maritime Service", description = "Maritime Service Internal API")
public class MaritimeServiceInternalController {

    private final MaritimeServiceRepository repository;

    @GetMapping("/sissy-ids")
    @Operation(summary = "Get the list of maritime service sissy ids", tags = "Maritime Service")
    public List<String> getSissyIds() {
        return repository.findAllSissyIds();
    }
}
