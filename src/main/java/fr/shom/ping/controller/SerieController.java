package fr.shom.ping.controller;

import fr.shom.ping.model.dto.SerieDto;
import fr.shom.ping.model.predicate.SerieSearchCriteria;
import fr.shom.ping.service.SerieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/series")
@RequiredArgsConstructor
@Tag(name = "Serie", description = "Serie API")
public class SerieController {

    private final SerieService service;

    @GetMapping
    @Operation(summary = "Get the list of series by search criteria", tags = "Serie")
    public List<SerieDto> getAllBySearchCriteria(SerieSearchCriteria serieSearchCriteria) {
        return service.findAllBySearchCriteria(serieSearchCriteria);
    }

}
