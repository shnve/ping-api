package fr.shom.ping.controller;

import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.model.StaticSection;
import fr.shom.ping.model.dto.StaticSectionDto;
import fr.shom.ping.repository.StaticSectionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/static-sections")
@RequiredArgsConstructor
@Tag(name = "Static section")
public class StaticSectionController {
    private final StaticSectionRepository repository;

    @GetMapping("/{key}")
    @Operation(summary = "Get static section by key", tags = "Static section")
    public StaticSectionDto findByKey(@PathVariable String key) {
        return repository.findOneByKey(key).map(StaticSection::asDto).orElseThrow(NotFoundException::new);
    }
}
