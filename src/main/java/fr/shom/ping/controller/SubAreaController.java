package fr.shom.ping.controller;

import fr.shom.ping.model.SubArea;
import fr.shom.ping.model.dto.SubAreaDetailsDto;
import fr.shom.ping.repository.SubAreaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sub-areas")
@RequiredArgsConstructor
@Tag(name = "Sub Area", description = "Sub Area API")
public class SubAreaController {
    private final SubAreaRepository repository;

    @GetMapping
    @Operation(summary = "Get the list of sub areas", tags = "Sub Area")
    public List<SubAreaDetailsDto> get() {
        return repository.findAll().stream().map(SubArea::asDto).collect(Collectors.toList());
    }
}
