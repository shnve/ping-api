package fr.shom.ping.controller;

import fr.shom.ping.model.dto.NotificationDto;
import fr.shom.ping.service.NotificationSecuredService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import java.security.Principal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/secured-api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification (Public User)", description = "Notification API")

public class NotificationSecuredController {

    private final NotificationSecuredService service;

    @GetMapping
    @SuppressWarnings("squid:S4834")
    @PreAuthorize("isAuthenticated()")
    public List<NotificationDto> getAllByPublicUser(@Parameter(hidden = true) Principal principal) {
        return service.findAllByPublicUser(principal.getName());
    }

    @PostMapping("/update")
    @SuppressWarnings("squid:S4834")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update notification read status", tags = "Notification")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    public void updateReadStatus(
        @RequestBody Set<UUID> ids,
        @RequestParam boolean readStatus,
        @Parameter(hidden = true) Principal principal) {
        service.updateReadStatus(ids, readStatus, principal.getName());
    }

    @DeleteMapping("/delete")
    @SuppressWarnings("squid:S4834")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete notification", tags = "Notification")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    public void delete(
        @RequestBody Set<UUID> ids,
        @Parameter(hidden = true) Principal principal) {
        service.delete(ids, principal.getName());
    }
}
