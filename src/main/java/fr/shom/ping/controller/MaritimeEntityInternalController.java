package fr.shom.ping.controller;

import fr.shom.ping.repository.MaritimeEntityRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/maritime-entities")
@RequiredArgsConstructor
@Tag(name = "Maritime Entity", description = "Maritime Entity Internal API")
public class MaritimeEntityInternalController {

    private final MaritimeEntityRepository repository;

    @GetMapping("/sissy-ids")
    @Operation(summary = "Get the list of maritime service sissy ids", tags = "Maritime Entity")
    public List<String> getSissyIds() {
        return repository.findAllSissyIds();
    }
}
