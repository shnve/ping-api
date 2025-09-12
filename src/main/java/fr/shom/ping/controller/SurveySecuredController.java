package fr.shom.ping.controller;

import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.dto.AbstractSurveyDepositCreateDto;
import fr.shom.ping.model.dto.AbstractSurveyTestCreateDto;
import fr.shom.ping.model.dto.SurveyCloneDto;
import fr.shom.ping.model.dto.SurveyDepositCreateDtoWithPublicUser;
import fr.shom.ping.model.dto.SurveyModalDtoPublicUser;
import fr.shom.ping.model.dto.SurveyTableDto;
import fr.shom.ping.model.dto.SurveyTestCreateDtoWithPublicUser;
import fr.shom.ping.model.predicate.SurveySearchCriteria;
import fr.shom.ping.service.SurveyService;
import fr.shom.ping.service.SurveyTransmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.security.Principal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/secured-api/surveys")
@RequiredArgsConstructor
@Tag(name = "Survey (Public User)", description = "Survey API")
public class SurveySecuredController {

    private final SurveyService service;

    private final SurveyTransmissionService transmissionService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get paginated surveys by search criteria (Table public View)", tags = "Survey (public)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, 
        example = "Bearer access_token")
    public ResponseEntity<PagedModel<EntityModel<SurveyTableDto>>> getAllBySearchCriteria(
        @Valid SurveySearchCriteria searchCriteria,
        Pageable pageable,
        PagedResourcesAssembler<SurveyTableDto> pagedResourcesAssembler,
        @Parameter(hidden = true) Principal principal) {
        final Page<SurveyTableDto> surveys = service.findAllBySearchCriteria(searchCriteria, pageable, principal.getName());
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(surveys));
    }

    @SuppressWarnings("squid:S4834")
    @PostMapping("/deposit")
    @ResponseStatus(CREATED)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create deposit survey", tags = "Survey (public)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    public UUID createDeposit(@Valid @RequestBody SurveyDepositCreateDtoWithPublicUser dto, @Parameter(hidden = true) Principal principal)
        throws ServiceException {
        return service.createDeposit(dto, principal.getName());
    }

    @SuppressWarnings("squid:S4834")
    @PutMapping(value = "/deposit/{id}", consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update deposit survey", tags = "Survey (public)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    public UUID updateDeposit(@PathVariable UUID id, @Validated @RequestBody SurveyDepositCreateDtoWithPublicUser dto,
        @Parameter(hidden = true) Principal principal) throws ServiceException {
        return service.updateDeposit(id, dto, principal.getName());
    }

    @GetMapping("/deposit/{id}")
    @Operation(summary = "Get the survey by ID", tags = "Survey (public)")
    @PreAuthorize("isAuthenticated()")
    public AbstractSurveyDepositCreateDto findDepositById(@PathVariable UUID id,
        @Parameter(hidden = true) Principal principal) {
        return service.findDepositById(id, principal.getName());
    }

    @DeleteMapping("/deposit/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete deposit draft", tags = "Survey (public)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    @ResponseStatus(NO_CONTENT)
    public void delete(@PathVariable UUID id, @Parameter(hidden = true) Principal principal) {
        service.deleteDraftDeposit(id, principal.getName());
    }

    @SuppressWarnings("squid:S4834")
    @PostMapping("/notdeposit")
    @ResponseStatus(CREATED)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create test survey", tags = "Survey (public)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    public UUID createTest(@Valid @RequestBody SurveyTestCreateDtoWithPublicUser dto, @Parameter(hidden = true) Principal principal)
        throws ServiceException {
        return service.createTest(dto, principal.getName());
    }

    @SuppressWarnings("squid:S4834")
    @PutMapping(value = "/notdeposit/{id}", consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update test survey", tags = "Survey (public)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    public UUID updateTest(@PathVariable UUID id, @Validated @RequestBody SurveyTestCreateDtoWithPublicUser dto,
        @Parameter(hidden = true) Principal principal) throws ServiceException {
        return service.updateTest(id, dto, principal.getName());
    }

    @GetMapping("/notdeposit/{id}")
    @Operation(summary = "Get the survey by ID", tags = "Survey (public)")
    @PreAuthorize("isAuthenticated()")
    public AbstractSurveyTestCreateDto findTestById(@PathVariable UUID id,
        @Parameter(hidden = true) Principal principal) {
        return service.findTestById(id, principal.getName());
    }

    @GetMapping("/{id}/details-view")
    @Operation(summary = "Get survey details to display modal", tags = "Survey (public)")
    @PreAuthorize("isAuthenticated()")
    public SurveyModalDtoPublicUser getForDetailsView(@PathVariable UUID id,
        @Parameter(hidden = true) Principal principal) {
        return service.findForDetailsView(id, principal.getName());
    }

    @PutMapping("/{id}/clone")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Clone a survey", tags = "Survey (public)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    public UUID clone(
        @PathVariable UUID id,
        @Valid @RequestBody SurveyCloneDto dto,
        @Parameter(hidden = true) Principal principal) {
        return service.clone(id, dto, principal.getName());
    }

    @PutMapping("/{id}/report")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Report a survey", tags = "Survey (public)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    public UUID report(@PathVariable UUID id,
        @Parameter(hidden = true) Principal principal) {
        return service.report(id, principal.getName());
    }

    @PutMapping(value = "/{id}/transmit", consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Transmit a draft deposit survey", tags = "Survey (public)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    public void transmitDepositDraft(@PathVariable UUID id, @Validated @RequestBody SurveyDepositCreateDtoWithPublicUser dto,
        @Parameter(hidden = true) Principal principal) throws ServiceException {
        transmissionService.transmit(id, dto, principal.getName());
    }
}
