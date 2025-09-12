package fr.shom.ping.controller;

import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.model.StaticLogo;
import fr.shom.ping.model.dto.StaticLogoDto;
import fr.shom.ping.repository.StaticLogoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/static-logos")
@RequiredArgsConstructor
@Tag(name = "Static logo")
public class StaticLogoController {
    private final StaticLogoRepository repository;

    @GetMapping("/{key}")
    @Operation(summary = "Get static logo by key", tags = "Static logo")
    public StaticLogoDto findByKey(@PathVariable String key) {
        return repository.findOneByKey(key).map(StaticLogo::asDto).orElseThrow(NotFoundException::new);
    }

}
