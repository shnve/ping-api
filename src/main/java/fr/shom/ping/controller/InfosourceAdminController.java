package fr.shom.ping.controller;

import fr.shom.ping.exception.BindingException;
import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.dto.InfosourceActivityForecastCreateWithServiceUserDto;
import fr.shom.ping.model.dto.InfosourceActivityForecastDetailWithServiceUserDto;
import fr.shom.ping.model.dto.InfosourceAdminDetailDto;
import fr.shom.ping.model.dto.InfosourceAdminTableDto;
import fr.shom.ping.model.dto.InfosourceBathyDangerReportCreateWithServiceUserDto;
import fr.shom.ping.model.dto.InfosourceFutureUpdatesCreateWithServiceUserDto;
import fr.shom.ping.model.dto.InfosourceHazardReportCreateWithServiceUserDto;
import fr.shom.ping.model.dto.InfosourceMapAnomalyCreateWithServiceUserDto;
import fr.shom.ping.model.dto.InfosourceOtherReportCreateWithServiceUserDto;
import fr.shom.ping.model.dto.InfosourcePublicationAnomalyCreateWithServiceUserDto;
import fr.shom.ping.model.dto.InfosourceReferenceDto;
import fr.shom.ping.model.dto.InfosourceReportAbstractCreateDto;
import fr.shom.ping.model.dto.InfosourceServiceStatusServiceUserDto;
import fr.shom.ping.model.dto.InfosourceToDisplayOnCreationMapDto;
import fr.shom.ping.model.dto.MaritimeEntityConcernedDto;
import fr.shom.ping.model.dto.MaritimeServiceInfosourceRecipientDto;
import fr.shom.ping.model.dto.UpdateInfosourceCommentDto;
import fr.shom.ping.model.dto.UpdateInfosourceServiceStatusDto;
import fr.shom.ping.model.enums.InfosourceState;
import fr.shom.ping.model.predicate.InfosourceAdminSearchCriteria;
import fr.shom.ping.service.InfosourceAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Geometry;
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
@RequestMapping("/admin-api/infosources")
@RequiredArgsConstructor
@Tag(name = "Infosource (Service User)", description = "Infosource API")
public class InfosourceAdminController {

    private static final String DEFAULT_LANGUAGE = "fr";

    private final InfosourceAdminService service;

    @GetMapping(value = "/for-creation-map")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get geometries for all infosources", tags = "Infosource (Service User)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,
        example = "Bearer access_token")
    public List<InfosourceToDisplayOnCreationMapDto> findAllToDisplayInCreationMap(@RequestParam(required = false) UUID infosourceId,
        @Parameter(hidden = true) Principal principal) {
        return service.findAllToDisplayOnCreationMap(infosourceId, principal.getName());
    }

    @PostMapping("/hazard-report")
    @ResponseStatus(CREATED)
    @PreAuthorize("hasAuthority('service-user') and hasPermission(#dto.serviceId, 'SERVICE_OPERATOR,SERVICE_ADMIN')")
    @Operation(summary = "Create infosource (Hazard Report)", tags = "Infosource (Service User)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,
        example = "Bearer access_token")
    public UUID createHazardReport(
        @Valid @RequestBody InfosourceHazardReportCreateWithServiceUserDto dto,
        @Parameter(hidden = true) BindingResult bindingResult,
        @Parameter(hidden = true) Principal principal) {
        try {
            return service.createHazardReport(dto, bindingResult, principal.getName());
        } catch (ServiceException ignored) {
            throw new BindingException(bindingResult.getAllErrors());
        }
    }

    @PostMapping("/other-report")
    @ResponseStatus(CREATED)
    @PreAuthorize("hasAuthority('service-user') and hasPermission(#dto.serviceId, 'SERVICE_OPERATOR,SERVICE_ADMIN')")
    @Operation(summary = "Create infosource (Other Report)", tags = "Infosource (Service User)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    public UUID createOtherReport(
        @Valid @RequestBody InfosourceOtherReportCreateWithServiceUserDto dto,
        @Parameter(hidden = true) BindingResult bindingResult,
        @Parameter(hidden = true) Principal principal) {
        try {
            return service.createOtherReport(dto, bindingResult, principal.getName());
        } catch (ServiceException ignored) {
            throw new BindingException(bindingResult.getAllErrors());
        }
    }

    @PostMapping("/map-anomaly")
    @ResponseStatus(CREATED)
    @PreAuthorize("hasAuthority('service-user') and hasPermission(#dto.serviceId, 'SERVICE_OPERATOR,SERVICE_ADMIN')")
    @Operation(summary = "Create infosource (Map Anomaly)", tags = "Infosource (Service User)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    public UUID createMapAnomaly(
        @Valid @RequestBody InfosourceMapAnomalyCreateWithServiceUserDto dto,
        @Parameter(hidden = true) BindingResult bindingResult,
        @Parameter(hidden = true) Principal principal) {
        try {
            return service.createMapAnomaly(dto, bindingResult, principal.getName());
        } catch (ServiceException ignored) {
            throw new BindingException(bindingResult.getAllErrors());
        }
    }

    @PostMapping("/future-updates")
    @ResponseStatus(CREATED)
    @PreAuthorize("hasAuthority('service-user') and hasPermission(#dto.serviceId, 'SERVICE_OPERATOR,SERVICE_ADMIN')")
    @Operation(summary = "Create infosource (Future Updates)", tags = "Infosource (Service User)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    public UUID createFutureUpdates(
        @Valid @RequestBody InfosourceFutureUpdatesCreateWithServiceUserDto dto,
        @Parameter(hidden = true) BindingResult bindingResult,
        @Parameter(hidden = true) Principal principal) {
        try {
            return service.createFutureUpdates(dto, bindingResult, principal.getName());
        } catch (ServiceException ignored) {
            throw new BindingException(bindingResult.getAllErrors());
        }
    }

    @PostMapping("/publication-anomaly")
    @ResponseStatus(CREATED)
    @PreAuthorize("hasAuthority('service-user') and hasPermission(#dto.serviceId, 'SERVICE_OPERATOR,SERVICE_ADMIN')")
    @Operation(summary = "Create infosource (Publication Anomaly)", tags = "Infosource (Service User)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    public UUID createPublicationAnomaly(
        @Valid @RequestBody InfosourcePublicationAnomalyCreateWithServiceUserDto dto,
        @Parameter(hidden = true) BindingResult bindingResult,
        @Parameter(hidden = true) Principal principal) {
        try {
            return service.createPublicationAnomaly(dto, bindingResult, principal.getName());
        } catch (ServiceException ignored) {
            throw new BindingException(bindingResult.getAllErrors());
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get the Infosource Report by ID", tags = "Infosource (Admin)")
    @PreAuthorize("hasAuthority('service-user')")
    public InfosourceReportAbstractCreateDto findInfosourceById(@PathVariable UUID id,
        @Parameter(hidden = true) Principal principal) {
        return service.findById(id, principal.getName());
    }

    @PutMapping("/{id}/comment")
    @Operation(summary = "Update the Infosource Comment by ID", tags = "Infosource (Admin)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    @PreAuthorize("hasAuthority('service-user')")
    public UUID updateComment(@PathVariable UUID id,
        @Validated @RequestBody UpdateInfosourceCommentDto dto,
        @Parameter(hidden = true) BindingResult bindingResult,
        @Parameter(hidden = true) Principal principal) {
        try {
            return service.updateComment(id, dto, bindingResult, principal.getName());
        } catch (ServiceException ignored) {
            throw new BindingException(bindingResult.getAllErrors());
        }
    }

    @PutMapping(value = "/hazard-report/{id}", consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @Operation(summary = "Update Infosource Hazard Report", tags = "Infosource (Admin)")
    @PreAuthorize("hasAuthority('service-user') and hasPermission(#dto.serviceId, 'SERVICE_OPERATOR,SERVICE_ADMIN')")
    public UUID updateHazardReport(@PathVariable UUID id,
        @Validated @RequestBody InfosourceHazardReportCreateWithServiceUserDto dto,
        @Parameter(hidden = true) BindingResult bindingResult,
        @Parameter(hidden = true) Principal principal) {
        try {
            return service.updateHazardReport(id, dto, bindingResult, principal.getName());
        } catch (ServiceException ignored) {
            throw new BindingException(bindingResult.getAllErrors());
        }
    }

    @PutMapping(value = "/other-report/{id}", consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @Operation(summary = "Update Infosource Other Report", tags = "Infosource (Admin)")
    @PreAuthorize("hasAuthority('service-user') and hasPermission(#dto.serviceId, 'SERVICE_OPERATOR,SERVICE_ADMIN')")
    public UUID updateOtherReport(@PathVariable UUID id,
        @Validated @RequestBody InfosourceOtherReportCreateWithServiceUserDto dto,
        @Parameter(hidden = true) BindingResult bindingResult,
        @Parameter(hidden = true) Principal principal) {
        try {
            return service.updateOtherReport(id, dto, bindingResult, principal.getName());
        } catch (ServiceException ignored) {
            throw new BindingException(bindingResult.getAllErrors());
        }
    }

    @PutMapping(value = "/map-anomaly/{id}", consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @Operation(summary = "Update Infosource Map Anomaly", tags = "Infosource (Admin)")
    @PreAuthorize("hasAuthority('service-user') and hasPermission(#dto.serviceId, 'SERVICE_OPERATOR,SERVICE_ADMIN')")
    public UUID updateMapAnomaly(@PathVariable UUID id,
        @Validated @RequestBody InfosourceMapAnomalyCreateWithServiceUserDto dto,
        @Parameter(hidden = true) BindingResult bindingResult,
        @Parameter(hidden = true) Principal principal) throws ServiceException {
        try {
            return service.updateMapAnomaly(id, dto, bindingResult, principal.getName());
        } catch (ServiceException ignored) {
            throw new BindingException(bindingResult.getAllErrors());
        }
    }

    @PutMapping(value = "/future-updates/{id}", consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @Operation(summary = "Update Infosource Future Updates", tags = "Infosource (Admin)")
    @PreAuthorize("hasAuthority('service-user') and hasPermission(#dto.serviceId, 'SERVICE_OPERATOR,SERVICE_ADMIN')")
    public UUID updateFutureUpdates(@PathVariable UUID id,
        @Validated @RequestBody InfosourceFutureUpdatesCreateWithServiceUserDto dto,
        @Parameter(hidden = true) BindingResult bindingResult,
        @Parameter(hidden = true) Principal principal) {
        try {
            return service.updateFutureUpdates(id, dto, bindingResult, principal.getName());
        } catch (ServiceException ignored) {
            throw new BindingException(bindingResult.getAllErrors());
        }
    }

    @PutMapping(value = "/publication-anomaly/{id}", consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @Operation(summary = "Update Infosource Publication Anomaly", tags = "Infosource (Admin)")
    @PreAuthorize("hasAuthority('service-user') and hasPermission(#dto.serviceId, 'SERVICE_OPERATOR,SERVICE_ADMIN')")
    public UUID updatePublicationAnomaly(@PathVariable UUID id,
        @Validated @RequestBody InfosourcePublicationAnomalyCreateWithServiceUserDto dto,
        @Parameter(hidden = true) BindingResult bindingResult,
        @Parameter(hidden = true) Principal principal) {
        try {
            return service.updatePublicationAnomaly(id, dto, bindingResult, principal.getName());
        } catch (ServiceException ignored) {
            throw new BindingException(bindingResult.getAllErrors());
        }
    }

    @PostMapping("/activity-forecast")
    @ResponseStatus(CREATED)
    @PreAuthorize("hasAuthority('service-user') and hasPermission(#dto.serviceId, 'SERVICE_OPERATOR,SERVICE_ADMIN')")
    @Operation(summary = "Create infosource (Activity forecast)", tags = "Infosource (Service User)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    public UUID createActivityForecast(
        @Valid @RequestBody InfosourceActivityForecastCreateWithServiceUserDto dto,
        @Parameter(hidden = true) BindingResult bindingResult,
        @Parameter(hidden = true) Principal principal) {
        try {
            return service.createActivityForecast(dto, bindingResult, principal.getName());
        } catch (ServiceException ignored) {
            throw new BindingException(bindingResult.getAllErrors());
        }
    }

    @GetMapping("/activity-forecast/{id}")
    @Operation(summary = "Get the Infosource Report by ID", tags = "Infosource (Admin)")
    @PreAuthorize("hasAuthority('service-user')")
    public InfosourceActivityForecastDetailWithServiceUserDto findActivityForecastInfosourceById(@PathVariable UUID id,
        @Parameter(hidden = true) Principal principal) {
        return service.findActivityForecastInfosourceById(id, principal.getName());
    }

    @PutMapping("/activity-forecast/{id}")
    @PreAuthorize("hasAuthority('service-user') and hasPermission(#dto.serviceId, 'SERVICE_OPERATOR,SERVICE_ADMIN')")
    @Operation(summary = "Update infosource (Activity forecast)", tags = "Infosource (Service User)")
    public UUID updateActivityForecast(
        @PathVariable UUID id,
        @Valid @RequestBody InfosourceActivityForecastDetailWithServiceUserDto dto,
        @Parameter(hidden = true) BindingResult bindingResult,
        @Parameter(hidden = true) Principal principal) {
        try {
            return service.updateActivityForecast(id, dto, bindingResult, principal.getName());
        } catch (ServiceException ignored) {
            throw new BindingException(bindingResult.getAllErrors());
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Get paginated infosources by search criteria (Table Pro View)", tags = "Infosource")
    @Parameter(name = "Authorization", description = "Access Token", required = true,
        example = "Bearer access_token")
    public ResponseEntity<PagedModel<EntityModel<InfosourceAdminTableDto>>> getAllBySearchCriteria(
        @Valid InfosourceAdminSearchCriteria searchCriteria,
        @RequestParam(defaultValue = DEFAULT_LANGUAGE) String lang,
        Pageable pageable,
        PagedResourcesAssembler<InfosourceAdminTableDto> pagedResourcesAssembler,
        @Parameter(hidden = true) Principal principal) {
        Page<InfosourceAdminTableDto> infosources = service.findAllBySearchCriteria(searchCriteria, pageable, principal.getName(), lang);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(infosources));
    }

    @GetMapping("{id}/details-view")
    @PreAuthorize("hasAuthority('service-user')")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    public InfosourceAdminDetailDto getForDetailsView(@PathVariable UUID id, @Parameter(hidden = true) Principal principal) {
        return service.findByIdForDetailsView(id, principal.getName());
    }

    @PutMapping("/{id}/send")
    @PreAuthorize("hasAuthority('service-user')")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    public void send(@PathVariable UUID id,
        @Valid @RequestBody Set<String> maritimeServiceIds,
        @Parameter(hidden = true) Principal principal) {
        service.send(id, principal.getName(), maritimeServiceIds);
    }

    @GetMapping("{id}/routing-services")
    @PreAuthorize("hasAuthority('service-user')")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    public Set<MaritimeServiceInfosourceRecipientDto> getRoutingServices(@PathVariable UUID id,
        @Parameter(hidden = true) Principal principal) {
        return service.getRoutingServices(id, principal.getName());
    }

    @GetMapping(value = "/available-recipients-sent")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Get available recipients for all sent infosources of issuing service", tags = "Infosource (Service User)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,
        example = "Bearer access_token")
    public List<MaritimeServiceInfosourceRecipientDto> getAvailableRecipientsFromSentInfosources(
        @RequestParam UUID issuingServiceId,
        @RequestParam InfosourceState state) {
        return service.getAvailableRecipientsFromSentInfosources(issuingServiceId, state);
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Reject infosource by id", tags = "Infosource (Service User)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    public void reject(@PathVariable UUID id, @RequestBody(required = false) String optionalComment,
        @Parameter(hidden = true) Principal principal) {
        service.reject(id, optionalComment, principal.getName());
    }

    @GetMapping("/{id}/service-statuses")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Get status by service", tags = "Infosource (Service User)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    public List<InfosourceServiceStatusServiceUserDto> findServiceStatuses(
        @PathVariable UUID id) {
        return service.findServiceStatuses(id);
    }

    @PutMapping("/{id}/service-statuses")
    @PreAuthorize("hasAuthority('service-user') and hasPermission(#dto.serviceId, 'SERVICE_OPERATOR,SERVICE_ADMIN')")
    @Operation(summary = "Update infosource service status by id", tags = "Infosource (Service User)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    public void updateServiceStatus(
        @PathVariable UUID id,
        @RequestBody UpdateInfosourceServiceStatusDto dto,
        @Parameter(hidden = true) Principal principal) {
        service.updateServiceStatus(id, dto, principal.getName());
    }

    @GetMapping("/{id}/transferable-services")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Get transferable services", tags = "Infosource (Service User)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    public Set<MaritimeServiceInfosourceRecipientDto> findTransferableServices(
        @PathVariable UUID id,
        @Parameter(hidden = true) Principal principal) {
        return service.findTransferableServices(id, principal.getName());
    }

    @PutMapping("/{id}/transfer")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Transfer infosource to service", tags = "Infosource (Service User)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    public void transfer(
        @PathVariable UUID id,
        @RequestBody List<UUID> serviceIds,
        @Parameter(hidden = true) Principal principal) {
        service.transfer(id, serviceIds, principal.getName());
    }

    @PutMapping("/{id}/create-warning")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Create warning from infosource", tags = "Infosource (Service User)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    public UUID createWarning(
        @PathVariable UUID id,
        @RequestParam UUID serieId,
        @RequestParam(required = false, defaultValue = "false") Boolean copyDataFromInfosource,
        @Parameter(hidden = true) Principal principal) {
        return service.createWarning(id, serieId, copyDataFromInfosource, principal.getName());
    }

    @GetMapping(value = "/available-infosources-to-link")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Get available infosources on which serie's service is recipient or issuer", tags = "Infosource (Service User)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,
        example = "Bearer access_token")
    public Set<InfosourceReferenceDto> getAvailableInfosourcesToLinkFromSerie(
        @RequestParam UUID serieId,
        @Parameter(hidden = true) Principal principal) {
        return service.getAvailableInfosourcesToLinkFromSerie(serieId, principal.getName());
    }

    @PostMapping("/available-entities-for-creation")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Get the list of maritime entities from service id and intersections", tags = "Infosource (Service User)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    public List<MaritimeEntityConcernedDto> getAvailableEntitiesForCreation(
        @RequestParam UUID serviceId,
        @RequestBody(required = false) List<Geometry> geometries,
        @Parameter(hidden = true) Principal principal) {
        return service.getAvailableEntitiesForCreation(serviceId, geometries, principal.getName());
    }

    @GetMapping(value = "/available-tags")
    @PreAuthorize("hasAuthority('service-user') and hasPermission(#issuingServiceId, '*')")
    @Operation(summary = "Get available tags for issuing service",
        tags = "Infosource (Service User)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,
        example = "Bearer access_token")
    public List<String> getAvailableTagsFromServiceId(@RequestParam UUID issuingServiceId) {
        return service.getAvailableTagsFromServiceId(issuingServiceId);
    }

    @PutMapping("/{id}/clone")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Clone an infosource", tags = "Infosource (Service User)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    public UUID clone(@PathVariable UUID id, @Parameter(hidden = true) Principal principal) {
        return service.clone(id, principal.getName());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Delete an infosource", tags = "Infosource (Service User)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    @ResponseStatus(NO_CONTENT)
    public void delete(@PathVariable UUID id, @Parameter(hidden = true) Principal principal) {
        service.delete(id, principal.getName());
    }

    @PutMapping(value = "/{id}/tags", consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @Operation(summary = "Update tags for infosource", tags = "Infosource (Service User)")
    @PreAuthorize("hasAuthority('service-user')")
    @ResponseStatus(NO_CONTENT)
    public void linkTagsToInfosource(@PathVariable UUID id,
        @RequestParam List<UUID> tagsIdsToLink, @Parameter(hidden = true) Principal principal) {
        service.linkTagsToInfosource(id, tagsIdsToLink, principal.getName());
    }

    @GetMapping(value = "/{id}/tags")
    @Operation(summary = "Get tags IDs from infosource", tags = "Infosource (Service User)")
    @PreAuthorize("hasAuthority('service-user')")
    public List<UUID> getTagsIdsFromInfosourceId(@PathVariable UUID id, @Parameter(hidden = true) Principal principal) {
        return service.getTagsIdsFromInfosourceId(id, principal.getName());
    }

    @PutMapping(value = "/bathy-report/{id}", consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @Operation(summary = "Update Infosource Bathy Report", tags = "Infosource (Admin)")
    @PreAuthorize("hasAuthority('service-user') and hasPermission(#dto.serviceId, 'SERVICE_OPERATOR,SERVICE_ADMIN')")
    public UUID updateBathyReport(@PathVariable UUID id,
        @Validated @RequestBody InfosourceBathyDangerReportCreateWithServiceUserDto dto,
        @Parameter(hidden = true) BindingResult bindingResult,
        @Parameter(hidden = true) Principal principal) {
        try {
            return service.updateBathyReport(id, dto, bindingResult, principal.getName());
        } catch (ServiceException ignored) {
            throw new BindingException(bindingResult.getAllErrors());
        }
    }
}
