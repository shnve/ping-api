package fr.shom.ping.controller;

import fr.shom.ping.service.TranslationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PISAC (18-nov-2026): propuesta de traducción para el campo «Información» de advertencias y regulaciones.
 * Solo usuarios institucionales.
 */
@RestController
@RequestMapping("/admin-api/translation")
@PreAuthorize("hasAuthority('service-user')")
@RequiredArgsConstructor
public class TranslationAdminController {

    private final TranslationService translationService;

    public record TranslationRequest(
        @Size(max = TranslationService.MAX_LENGTH) String text,
        @NotBlank String source,
        @NotBlank String target) {
    }

    public record TranslationResponse(String text) {
    }

    @PostMapping
    public TranslationResponse translate(@Valid @RequestBody TranslationRequest request) {
        return new TranslationResponse(translationService.translate(request.text(), request.source(), request.target()));
    }
}
