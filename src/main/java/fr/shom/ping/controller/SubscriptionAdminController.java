package fr.shom.ping.controller;

import fr.shom.ping.model.dto.SubscriptionCreateDto;
import fr.shom.ping.model.dto.SubscriptionDto;
import fr.shom.ping.model.dto.SubscriptionTableDto;
import fr.shom.ping.model.predicate.SubscriptionSearchCriteria;
import fr.shom.ping.service.SubscriptionAdminService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/admin-api/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscription", description = "Subscription Admin API")
public class SubscriptionAdminController {

    private final SubscriptionAdminService service;

    @GetMapping
    @SuppressWarnings("squid:S4834")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Get paginated subscriptions by search criteria (Table Public View)", tags = "Subscription")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    public ResponseEntity<PagedModel<EntityModel<SubscriptionTableDto>>> getAllBySearchCriteria(
        @Valid SubscriptionSearchCriteria searchCriteria,
        Pageable pageable,
        PagedResourcesAssembler<SubscriptionTableDto> pagedResourcesAssembler,
        @Parameter(hidden = true) Principal principal) {
        Page<SubscriptionTableDto> subscriptions = service.findAllBySearchCriteria(searchCriteria, pageable, principal.getName());
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(subscriptions));
    }

    @PostMapping("/warning")
    @SuppressWarnings("squid:S4834")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Create a warning subscription", tags = "Subscription")
    @Parameter(name = "Authorization", description = "Access Token", required = true, 
        example = "Bearer access_token")
    public UUID createWarningSubscription(@RequestBody @Valid SubscriptionCreateDto dto, @Parameter(hidden = true) Principal principal) {
        return service.createWarningSubscription(dto, principal.getName());
    }

    @PostMapping("/georegulation")
    @SuppressWarnings("squid:S4834")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Create a georegulation subscription", tags = "Subscription")
    @Parameter(name = "Authorization", description = "Access Token", required = true, 
        example = "Bearer access_token")
    public UUID createGeoregulationSubscription(
        @RequestBody @Valid SubscriptionCreateDto dto,
        @Parameter(hidden = true) Principal principal) {
        return service.createGeoregulationSubscription(dto, principal.getName());
    }

    @GetMapping("/{id}")
    @SuppressWarnings("squid:S4834")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Get a subscription based on an id", tags = "Subscription")
    @Parameter(name = "Authorization", description = "Access Token", required = true, 
        example = "Bearer access_token")
    public SubscriptionDto getById(@PathVariable UUID id, @Parameter(hidden = true) Principal principal) {
        return service.findById(id, principal.getName());
    }

    @DeleteMapping("/{id}")
    @SuppressWarnings("squid:S4834")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Delete a subscription", tags = "Subscription")
    @ResponseStatus(NO_CONTENT)
    public void delete(@PathVariable UUID id, @Parameter(hidden = true) Principal principal) {
        service.delete(id, principal.getName());
    }
}
