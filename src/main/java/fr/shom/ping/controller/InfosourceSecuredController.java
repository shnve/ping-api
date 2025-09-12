package fr.shom.ping.controller;

import fr.shom.ping.exception.BindingException;
import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.dto.InfosourceActivityForecastCreateWithPublicUserDto;
import fr.shom.ping.model.dto.InfosourceActivityForecastDetailWithPublicUserDto;
import fr.shom.ping.model.dto.InfosourceBathyDangerReportCreateWithPublicUserDto;
import fr.shom.ping.model.dto.InfosourceDetailDto;
import fr.shom.ping.model.dto.InfosourceFutureUpdatesCreateWithPublicUserDto;
import fr.shom.ping.model.dto.InfosourceHazardReportCreateWithPublicUserDto;
import fr.shom.ping.model.dto.InfosourceMapAnomalyCreateWithPublicUserDto;
import fr.shom.ping.model.dto.InfosourceOtherReportCreateWithPublicUserDto;
import fr.shom.ping.model.dto.InfosourcePublicationAnomalyCreateWithPublicUserDto;
import fr.shom.ping.model.dto.InfosourcePublicationAnomalyReportDetailWithPublicUserDto;
import fr.shom.ping.model.dto.InfosourceReportAbstractCreateDto;
import fr.shom.ping.model.dto.InfosourceServiceStatusServiceUserDto;
import fr.shom.ping.model.dto.InfosourceTableDto;
import fr.shom.ping.model.dto.InfosourceToDisplayOnCreationMapDto;
import fr.shom.ping.model.dto.ReferencesByFolderDto;
import fr.shom.ping.model.predicate.InfosourceSearchCriteria;
import fr.shom.ping.service.InfosourceSecuredService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/secured-api/infosources")
@RequiredArgsConstructor
@Tag(name = "Infosource (Public User)", description = "Infosource API")
public class InfosourceSecuredController {

    private static final String DEFAULT_LANGUAGE = "fr";

    private final InfosourceSecuredService service;

    @GetMapping(value = "/for-creation-map")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get geometries for all infosources", tags = "Infosource (Public User)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,
        example = "Bearer access_token")
    public List<InfosourceToDisplayOnCreationMapDto> findAllToDisplayInCreationMap(@RequestParam(required = false) UUID infosourceId,
        @Parameter(hidden = true) Principal principal) {
        return service.findAllToDisplayOnCreationMap(infosourceId, principal.getName());
    }

    @PostMapping(value = "/hazard-report")
    @ResponseStatus(CREATED)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create infosource (Hazard Report)", tags = "Infosource (Public User)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,
        example = "Bearer access_token")
    public UUID createHazardReport(
        @Valid @RequestBody InfosourceHazardReportCreateWithPublicUserDto dto,
        @Parameter(hidden = true) BindingResult bindingResult,
        @Parameter(hidden = true) Principal principal) {
        try {
            return service.createHazardReport(dto, bindingResult, principal.getName());
        } catch (ServiceException ignored) {
            throw new BindingException(bindingResult.getAllErrors());
        }
    }

    @PostMapping(value = "/other-report")
    @ResponseStatus(CREATED)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create infosource (Other Report)", tags = "Infosource (Public User)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    public UUID createOtherReport(
        @Valid @RequestBody InfosourceOtherReportCreateWithPublicUserDto dto,
        @Parameter(hidden = true) BindingResult bindingResult,
        @Parameter(hidden = true) Principal principal) {
        try {
            return service.createOtherReport(dto, bindingResult, principal.getName());
        } catch (ServiceException ignored) {
            throw new BindingException(bindingResult.getAllErrors());
        }
    }

    @PostMapping(value = "/map-anomaly")
    @ResponseStatus(CREATED)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create infosource (Map Anomaly)", tags = "Infosource (Public User)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    public UUID createMapAnomaly(
        @Valid @RequestBody InfosourceMapAnomalyCreateWithPublicUserDto dto,
        @Parameter(hidden = true) BindingResult bindingResult,
        @Parameter(hidden = true) Principal principal) {
        try {
            return service.createMapAnomaly(dto, bindingResult, principal.getName());
        } catch (ServiceException ignored) {
            throw new BindingException(bindingResult.getAllErrors());
        }
    }

    @PostMapping(value = "/future-updates")
    @ResponseStatus(CREATED)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create infosource (Future Updates)", tags = "Infosource (Public User)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    public UUID createFutureUpdates(
        @Valid @RequestBody InfosourceFutureUpdatesCreateWithPublicUserDto dto,
        @Parameter(hidden = true) BindingResult bindingResult,
        @Parameter(hidden = true) Principal principal) {
        try {
            return service.createFutureUpdates(dto, bindingResult, principal.getName());
        } catch (ServiceException ignored) {
            throw new BindingException(bindingResult.getAllErrors());
        }
    }

    @PostMapping(value = "/publication-anomaly")
    @ResponseStatus(CREATED)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create infosource (Publication Anomaly)", tags = "Infosource (Public User)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    public UUID createPublicationAnomaly(
        @Valid @RequestBody InfosourcePublicationAnomalyCreateWithPublicUserDto dto,
        @Parameter(hidden = true) BindingResult bindingResult,
        @Parameter(hidden = true) Principal principal) {
        try {
            return service.createPublicationAnomaly(dto, bindingResult, principal.getName());
        } catch (ServiceException ignored) {
            throw new BindingException(bindingResult.getAllErrors());
        }
    }

    @PutMapping(value = "/hazard-report/{id}", consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @Operation(summary = "Update Infosource Hazard Report", tags = "Infosource (Public User)")
    @PreAuthorize("isAuthenticated()")
    public UUID updateHazardReport(@PathVariable UUID id,
        @Validated @RequestBody InfosourceHazardReportCreateWithPublicUserDto dto,
        @Parameter(hidden = true) BindingResult bindingResult,
        @Parameter(hidden = true) Principal principal) throws ServiceException {
        return service.updateHazardReport(id, dto, bindingResult, principal.getName());
    }

    @PutMapping(value = "/other-report/{id}", consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @Operation(summary = "Update Infosource Other Report", tags = "Infosource (Public User)")
    @PreAuthorize("isAuthenticated()")
    public UUID updateOtherReport(@PathVariable UUID id,
        @Validated @RequestBody InfosourceOtherReportCreateWithPublicUserDto dto,
        @Parameter(hidden = true) BindingResult bindingResult,
        @Parameter(hidden = true) Principal principal) throws ServiceException {
        return service.updateOtherReport(id, dto, bindingResult, principal.getName());
    }

    @PutMapping(value = "/map-anomaly/{id}", consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @Operation(summary = "Update Infosource Map Anomaly", tags = "Infosource (Public User)")
    @PreAuthorize("isAuthenticated()")
    public UUID updateMapAnomaly(@PathVariable UUID id,
        @Validated @RequestBody InfosourceMapAnomalyCreateWithPublicUserDto dto,
        @Parameter(hidden = true) BindingResult bindingResult,
        @Parameter(hidden = true) Principal principal) throws ServiceException {
        return service.updateMapAnomaly(id, dto, bindingResult, principal.getName());
    }

    @PutMapping(value = "/future-updates/{id}", consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @Operation(summary = "Update Infosource Future Updates", tags = "Infosource (Public User)")
    @PreAuthorize("isAuthenticated()")
    public UUID updateFutureUpdates(@PathVariable UUID id,
        @Validated @RequestBody InfosourceFutureUpdatesCreateWithPublicUserDto dto,
        @Parameter(hidden = true) BindingResult bindingResult,
        @Parameter(hidden = true) Principal principal) throws ServiceException {
        return service.updateFutureUpdates(id, dto, bindingResult, principal.getName());
    }

    @PutMapping(value = "/publication-anomaly/{id}", consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @Operation(summary = "Update Infosource Publication Anomaly", tags = "Infosource (Public User)")
    @PreAuthorize("isAuthenticated()")
    public UUID updatePublicationAnomaly(@PathVariable UUID id,
        @Validated @RequestBody InfosourcePublicationAnomalyReportDetailWithPublicUserDto dto,
        @Parameter(hidden = true) BindingResult bindingResult,
        @Parameter(hidden = true) Principal principal) throws ServiceException {
        return service.updatePublicationAnomaly(id, dto, bindingResult, principal.getName());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get the Infosource Report by ID", tags = "Infosource (Public User)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    public InfosourceReportAbstractCreateDto findInfosourceById(
        @PathVariable UUID id,
        @Parameter(hidden = true) Principal principal) {
        return service.findById(id, principal.getName());
    }

    @GetMapping("/activity-forecast/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get the Infosource Report by ID", tags = "Infosource (Public User)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,
        example = "Bearer access_token")
    public InfosourceActivityForecastDetailWithPublicUserDto findActivityForecastInfosourceById(@PathVariable UUID id,
        @Parameter(hidden = true) Principal principal) {
        return service.findActivityForecastInfosourceById(id, principal.getName());
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get paginated infosources by search criteria (Table Public View)", tags = "Infosource")
    @Parameter(name = "Authorization", description = "Access Token", required = true,
        example = "Bearer access_token")
    public ResponseEntity<PagedModel<EntityModel<InfosourceTableDto>>> getAllBySearchCriteria(
        @Valid InfosourceSearchCriteria searchCriteria,
        @RequestParam(defaultValue = DEFAULT_LANGUAGE) String lang,
        Pageable pageable,
        PagedResourcesAssembler<InfosourceTableDto> pagedResourcesAssembler,
        @Parameter(hidden = true) Principal principal) {
        Page<InfosourceTableDto> infosources = service.findAllBySearchCriteria(searchCriteria, pageable, principal.getName(), lang);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(infosources));
    }

    @GetMapping("{id}/details-view")
    @PreAuthorize("isAuthenticated()")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    public InfosourceDetailDto getForDetailsView(@PathVariable UUID id, @Parameter(hidden = true) Principal principal) {
        return service.findByIdForDetailsView(id, principal.getName());
    }

    @PutMapping("/{id}/send")
    @PreAuthorize("isAuthenticated()")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    public void send(@PathVariable UUID id,
        @Parameter(hidden = true) Principal principal) {
        service.send(id, principal.getName());
    }

    @GetMapping("/{id}/service-statuses")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get status by service", tags = "Infosource (Public User)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    public List<InfosourceServiceStatusServiceUserDto> findServiceStatuses(
        @PathVariable UUID id,
        @Parameter(hidden = true) Principal principal) {
        return service.findServiceStatuses(id, principal.getName());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete infosource", tags = "Infosource (Public User)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    @ResponseStatus(NO_CONTENT)
    public void delete(@PathVariable UUID id, @Parameter(hidden = true) Principal principal) {
        service.delete(id, principal.getName());
    }

    @PostMapping("/activity-forecast")
    @ResponseStatus(CREATED)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create infosource (Activity forecast)", tags = "Infosource (Public User)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    public UUID createActivityForecast(
        @Valid @RequestBody InfosourceActivityForecastCreateWithPublicUserDto dto,
        @Parameter(hidden = true) BindingResult bindingResult,
        @Parameter(hidden = true) Principal principal) {
        try {
            return service.createActivityForecast(dto, bindingResult, principal.getName());
        } catch (ServiceException ignored) {
            throw new BindingException(bindingResult.getAllErrors());
        }
    }

    @PutMapping("/activity-forecast/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update infosource (Activity forecast)", tags = "Infosource (Public User)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    public UUID updateActivityForecast(
        @PathVariable UUID id,
        @Valid @RequestBody InfosourceActivityForecastDetailWithPublicUserDto dto,
        @Parameter(hidden = true) BindingResult bindingResult,
        @Parameter(hidden = true) Principal principal) throws ServiceException {
        return service.updateActivityForecast(id, dto, bindingResult, principal.getName());
    }

    @PutMapping(value = "/bathy-report/{id}", consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @Operation(summary = "Update Infosource Bathy Report", tags = "Infosource (Public User)")
    @PreAuthorize("isAuthenticated()")
    public UUID updateBathyReport(@PathVariable UUID id,
        @Validated @RequestBody InfosourceBathyDangerReportCreateWithPublicUserDto dto,
        @Parameter(hidden = true) BindingResult bindingResult,
        @Parameter(hidden = true) Principal principal) {
        try {
            return service.updateBathyReport(id, dto, bindingResult, principal.getName());
        } catch (ServiceException ignored) {
            throw new BindingException(bindingResult.getAllErrors());
        }
    }

    @GetMapping("/folder-references")
    @Operation(summary = "Get the references from folders link to infosource by ID", tags = "Infosource (Public User)")
    @PreAuthorize("isAuthenticated()")
    public Set<ReferencesByFolderDto> getAllReferencesByFolderLinkToInfosource(@RequestParam UUID infosourceId) {
        return service.getReferencesByFolderLinkedToInfosource(infosourceId);
    }
}
