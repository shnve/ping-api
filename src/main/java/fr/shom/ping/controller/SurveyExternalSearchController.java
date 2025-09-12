package fr.shom.ping.controller;

import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.properties.PingProperties;
import fr.shom.ping.service.SurveyAdminService;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.Operation;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/search/v1/survey")
@RequiredArgsConstructor
@Slf4j
public class SurveyExternalSearchController {
    private static final String X_SSL_CLIENT_FINGERPRINT_HEADER = "X-SSL-Client-Fingerprint";

    private final PingProperties pingProperties;

    private final SurveyAdminService service;

    @PutMapping(value = "/{id}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Set shomId to survey", tags = "Survey (external)")
    public ResponseEntity<Object> updateSurveyShomId(@PathVariable UUID id,
        @Valid @RequestBody @NoHtml @NotEmpty(message = "shomId is required") String shomId,
        @RequestHeader(value = X_SSL_CLIENT_FINGERPRINT_HEADER, required = false) String clientFingerprint) {
        if (checkInvalidFingerprint(clientFingerprint)) {
            return ResponseEntity.status(FORBIDDEN).build();
        }

        try {
            service.updateShomId(id, shomId);
        } catch (ServiceException e) {
            log.error("Errors during survey shomId update", e);
            return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(e);
        }

        return ResponseEntity.ok().build();
    }

    private boolean checkInvalidFingerprint(String clientFingerprint) {
        return !pingProperties.isBypassExternalSearchFingerprint()
            && (isEmpty(clientFingerprint) || !pingProperties.getAuthorizedCertificateFingerprints().contains(clientFingerprint.toLowerCase()));
    }
}
