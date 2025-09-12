package fr.shom.ping.controller;

import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.model.MaritimeEntity;
import fr.shom.ping.model.dto.MaritimeEntityDto;
import fr.shom.ping.repository.MaritimeEntityRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/maritime-entities")
@RequiredArgsConstructor
@Tag(name = "Maritime Entity", description = "Maritime Entity API")
public class MaritimeEntityController {

    private final MaritimeEntityRepository repository;

    @GetMapping("/{id}")
    public MaritimeEntityDto findById(@PathVariable UUID id) {
        return repository.findById(id).map(MaritimeEntity::asDto)
            .orElseThrow(NotFoundException::new);
    }

    @GetMapping
    @Operation(summary = "Get the list of maritime entities", tags = "Maritime Entity")
    public List<MaritimeEntityDto> get() {
        return repository.findAll().stream().map(MaritimeEntity::asDto).collect(Collectors.toList());
    }
}
