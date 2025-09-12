package fr.shom.ping.controller;

import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.dto.SurveyAnalysisProgressDto;
import fr.shom.ping.model.dto.SurveyMetadataDlLinkDto;
import fr.shom.ping.model.dto.SurveyResultFailureDto;
import fr.shom.ping.model.dto.SurveyResultSuccessDto;
import fr.shom.ping.properties.PingProperties;
import fr.shom.ping.service.SurveyExternalAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
@RequestMapping("/v1/survey")
@RequiredArgsConstructor
@Slf4j
public class SurveyExternalAnalysisController {
    private static final String X_SSL_CLIENT_FINGERPRINT_HEADER = "X-SSL-Client-Fingerprint";

    private final PingProperties pingProperties;

    private final SurveyExternalAnalysisService service;

    @PostMapping(value = "/{id}/success", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Notify a survey result", tags = "Survey (external)")
    public ResponseEntity<Object> resultAvailable(@PathVariable UUID id,
        @Validated @RequestBody List<SurveyResultSuccessDto> resultsInfo,
        @RequestHeader(value = X_SSL_CLIENT_FINGERPRINT_HEADER, required = false) String clientFingerprint) {
        if (checkInvalidFingerprint(clientFingerprint)) {
            return ResponseEntity.status(FORBIDDEN).build();
        }

        log.debug("Result available for survey {}", id);

        try {
            service.processAvailableResult(id, resultsInfo);
        } catch (ServiceException e) {
            log.error("Errors during survey result processing : {}", e);
            return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(e);
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/{id}/fail", consumes = APPLICATION_JSON_VALUE)
    @Operation(summary = "Notify a survey analysis failure", tags = "Survey (external)")
    public ResponseEntity<Object> analysisFailure(@PathVariable UUID id,
        @Validated @RequestBody SurveyResultFailureDto resultFailureDto,
        @RequestHeader(value = X_SSL_CLIENT_FINGERPRINT_HEADER, required = false) String clientFingerprint) {
        if (checkInvalidFingerprint(clientFingerprint)) {
            return ResponseEntity.status(FORBIDDEN).build();
        }

        log.debug("Analysis failure for survey {}", id);

        try {
            service.processAnalysisFailure(id, resultFailureDto);
        } catch (ServiceException e) {
            log.error("Errors during survey failure result processing : {}", e);
            return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(e);
        }

        return ResponseEntity.ok().build();
    }

    private boolean checkInvalidFingerprint(String clientFingerprint) {
        return !pingProperties.isBypassExternalAnalysisFingerprint()
            && (isEmpty(clientFingerprint) || !pingProperties.getAuthorizedCertificateFingerprints().contains(clientFingerprint.toLowerCase()));
    }


    @GetMapping(value = "/{id}", produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Fetch survey metadata and download link", tags = "Survey (external)")
    public ResponseEntity<SurveyMetadataDlLinkDto> getMetadataAndDownloadLink(@PathVariable UUID id,
        @RequestHeader(value = X_SSL_CLIENT_FINGERPRINT_HEADER, required = false) String clientFingerprint) {
        if (checkInvalidFingerprint(clientFingerprint)) {
            return ResponseEntity.status(FORBIDDEN).build();
        }
        final SurveyMetadataDlLinkDto result = service.getMetadataAndDownloadLink(id);
        return ResponseEntity.ok().body(result);
    }

    @PutMapping(value = "/{id}/status", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Set survey analysis progress", tags = "Survey (external)")
    public ResponseEntity<Object> setAnalysisProgress(@PathVariable UUID id,
        @Validated @RequestBody SurveyAnalysisProgressDto progressDto,
        @RequestHeader(value = X_SSL_CLIENT_FINGERPRINT_HEADER, required = false) String clientFingerprint) {
        if (checkInvalidFingerprint(clientFingerprint)) {
            return ResponseEntity.status(FORBIDDEN).build();
        }

        log.debug("Set processing progress for survey {}", id);

        try {
            service.setProcessingProgressStatus(id, progressDto);
        } catch (ServiceException e) {
            log.error("Errors during survey processing progress update : {}", e);
            return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(e);
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping
    @Operation(summary = "Get IDs of available surveys for analysis", tags = "Survey (external)")
    public ResponseEntity<Set<UUID>> findAvailableSurveyForAnalysis(
        @RequestHeader(value = X_SSL_CLIENT_FINGERPRINT_HEADER, required = false) String clientFingerprint
    ) {
        if (checkInvalidFingerprint(clientFingerprint)) {
            return ResponseEntity.status(FORBIDDEN).build();
        }

        final Set<UUID> result = service.findAvailableSurveyForAnalysis();

        return ResponseEntity.ok().body(result);
    }
}
