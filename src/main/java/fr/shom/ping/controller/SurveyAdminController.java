package fr.shom.ping.controller;

import fr.shom.ping.exception.BadRequestException;
import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.dto.AbstractSurveyDepositCreateDto;
import fr.shom.ping.model.dto.AbstractSurveyTestCreateDto;
import fr.shom.ping.model.dto.SurveyBriInternalCommentDto;
import fr.shom.ping.model.dto.SurveyCloneDto;
import fr.shom.ping.model.dto.SurveyDepositCreateDtoWithServiceUser;
import fr.shom.ping.model.dto.SurveyModalDtoServiceUser;
import fr.shom.ping.model.dto.SurveyTableBriDto;
import fr.shom.ping.model.dto.SurveyTableDto;
import fr.shom.ping.model.dto.SurveyTestCreateDtoWithServiceUser;
import fr.shom.ping.model.predicate.SurveyAdminBriSearchCriteria;
import fr.shom.ping.model.predicate.SurveyAdminSearchCriteria;
import fr.shom.ping.service.SurveyAdminService;
import fr.shom.ping.service.SurveyTransmissionAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.validation.BindingResult;
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
@RequestMapping("/admin-api/surveys")
@RequiredArgsConstructor
@Tag(name = "Survey (Service User)", description = "Survey API")
public class SurveyAdminController {

    private final SurveyAdminService service;

    private final SurveyTransmissionAdminService transmissionService;

    @GetMapping
    @PreAuthorize("hasAuthority('service-user') and hasPermission(#searchCriteria.issuingServiceId, '*')")
    @Operation(summary = "Get paginated surveys by search criteria (Table Pro View)", tags = "Survey")
    @Parameter(name = "Authorization", description = "Access Token", required = true, 
        example = "Bearer access_token")
    public ResponseEntity<PagedModel<EntityModel<SurveyTableDto>>> getAllBySearchCriteria(
        @Valid SurveyAdminSearchCriteria searchCriteria,
        Pageable pageable,
        PagedResourcesAssembler<SurveyTableDto> pagedResourcesAssembler,
        @Parameter(hidden = true) Principal principal) {
        final Page<SurveyTableDto> surveys = service.findAllBySearchCriteria(searchCriteria, pageable, principal.getName());
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(surveys));
    }

    @GetMapping("/bri")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Get paginated surveys by search criteria (BRI)", tags = "Survey")
    @Parameter(name = "Authorization", description = "Access Token", required = true, 
        example = "Bearer access_token")
    public ResponseEntity<PagedModel<EntityModel<SurveyTableBriDto>>> getAllBySearchCriteriaForBri(
        @Valid SurveyAdminBriSearchCriteria searchCriteria,
        Pageable pageable,
        PagedResourcesAssembler<SurveyTableBriDto> pagedResourcesAssembler,
        @Parameter(hidden = true) Principal principal) {
        final Page<SurveyTableBriDto> surveys = service.findAllBySearchCriteriaForBri(searchCriteria, pageable, principal.getName());
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(surveys));
    }

    @SuppressWarnings("squid:S4834")
    @PostMapping("/deposit")
    @ResponseStatus(CREATED)
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Create deposit survey", tags = "Survey (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    public UUID createDeposit(@Valid @RequestBody SurveyDepositCreateDtoWithServiceUser dto, @Parameter(hidden = true) Principal principal)
        throws ServiceException {
        return service.createDeposit(dto, principal.getName());
    }

    @SuppressWarnings("squid:S4834")
    @PutMapping(value = "/deposit/{id}", consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Update deposit survey", tags = "Survey (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    public UUID updateDeposit(@PathVariable UUID id, @Validated @RequestBody SurveyDepositCreateDtoWithServiceUser dto,
        @Parameter(hidden = true) Principal principal) throws ServiceException {
        return service.updateDeposit(id, dto, principal.getName());
    }

    @GetMapping("/deposit/{id}")
    @Operation(summary = "Get the deposit survey by ID", tags = "Survey (Admin)")
    @PreAuthorize("hasAuthority('service-user')")
    public AbstractSurveyDepositCreateDto findDepositById(@PathVariable UUID id,
        @Parameter(hidden = true) Principal principal) {
        return service.findDepositById(id, principal.getName());
    }

    @DeleteMapping("/deposit/{id}")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Delete deposit draft", tags = "Survey (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    @ResponseStatus(NO_CONTENT)
    public void delete(@PathVariable UUID id, @Parameter(hidden = true) Principal principal) {
        service.deleteDraftDeposit(id, principal.getName());
    }

    @SuppressWarnings("squid:S4834")
    @PostMapping("/notdeposit")
    @ResponseStatus(CREATED)
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Create test survey", tags = "Survey (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    public UUID createTest(@Valid @RequestBody SurveyTestCreateDtoWithServiceUser dto, @Parameter(hidden = true) Principal principal)
        throws ServiceException {
        return service.createTest(dto, principal.getName());
    }

    @SuppressWarnings("squid:S4834")
    @PutMapping(value = "/notdeposit/{id}", consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Update test survey", tags = "Survey (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    public UUID updateTest(@PathVariable UUID id, @Validated @RequestBody SurveyTestCreateDtoWithServiceUser dto,
        @Parameter(hidden = true) Principal principal) throws ServiceException {
        return service.updateTest(id, dto, principal.getName());
    }

    @GetMapping("/notdeposit/{id}")
    @Operation(summary = "Get the test survey by ID", tags = "Survey (Admin)")
    @PreAuthorize("hasAuthority('service-user')")
    public AbstractSurveyTestCreateDto findTestById(@PathVariable UUID id,
        @Parameter(hidden = true) Principal principal) {
        return service.findTestById(id, principal.getName());
    }

    @GetMapping("/{id}/details-view")
    @Operation(summary = "Get survey details to display modal", tags = "Survey (Admin)")
    @PreAuthorize("hasAuthority('service-user')")
    public SurveyModalDtoServiceUser getForDetailsView(@PathVariable UUID id,
        @Parameter(hidden = true) Principal principal) {
        return service.findForDetailsView(id, principal.getName());
    }

    @PutMapping("/{id}/clone")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Clone a survey", tags = "Survey (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    public UUID clone(
        @PathVariable UUID id,
        @Valid @RequestBody SurveyCloneDto dto,
        @Parameter(hidden = true) Principal principal) {
        return service.clone(id, dto, principal.getName());
    }

    @PutMapping("/{id}/report")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Report a survey", tags = "Survey (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    public UUID report(@PathVariable UUID id,
        @Parameter(hidden = true) Principal principal) {
        return service.report(id, principal.getName());
    }

    @PutMapping(value = "/{id}/transmit", consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Transmit a draft deposit survey", tags = "Survey (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    public void transmitDepositDraft(@PathVariable UUID id, @Validated @RequestBody SurveyDepositCreateDtoWithServiceUser dto,
        @Parameter(hidden = true) Principal principal) throws ServiceException {
        transmissionService.transmit(id, dto, principal.getName());
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Get survey zip archive of survey for BRI", tags = "Survey (Admin)")
    @PreAuthorize("hasAuthority('service-user')")
    public void downloadZipArchiveForBri(@PathVariable UUID id,
        @Parameter(hidden = true) HttpServletResponse response,
        @Parameter(hidden = true) Principal principal) {
        try {
            response.setContentType("application/zip");
            service.downloadZipArchiveForBri(id, principal.getName(), response);
        } catch (ServiceException exception) {
            throw new BadRequestException(exception.getMessage());
        }
    }

    @PutMapping("/{id}/comment")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Update BRI internal comment", tags = "Survey (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    public UUID updateBriInternalComment(@PathVariable UUID id,
        @Validated @RequestBody SurveyBriInternalCommentDto dto,
        @Parameter(hidden = true) BindingResult bindingResult,
        @Parameter(hidden = true) Principal principal) throws ServiceException {
        return service.updateBriInternalComment(id, dto, bindingResult, principal.getName());
    }
}
