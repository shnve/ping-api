package fr.shom.ping.controller;

import fr.shom.ping.service.WarningInternalService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/warnings")
@RequiredArgsConstructor
@Tag(name = "Warnings (internal)")
public class WarningInternalController {

    private final WarningInternalService service;

    @GetMapping("/telex")
    public String getConcatenatedTelexForAllPublishedWarningsBySerieId(@RequestParam UUID serieId) {
        return service.getConcatenatedTelexForAllPublishedWarningsBySerieId(serieId);
    }

}
