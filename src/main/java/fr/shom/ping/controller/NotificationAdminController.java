package fr.shom.ping.controller;

import fr.shom.ping.model.dto.NotificationDto;
import fr.shom.ping.service.NotificationAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/admin-api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification (Service User)", description = "Notification API")

public class NotificationAdminController {

    private final NotificationAdminService service;

    @GetMapping
    @SuppressWarnings("squid:S4834")
    @PreAuthorize("hasAuthority('service-user')")
    public List<NotificationDto> getAllByServiceUser(@Parameter(hidden = true) Principal principal) {
        return service.findAllByServiceUser(principal.getName());
    }

    @PostMapping("/update")
    @SuppressWarnings("squid:S4834")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Update notification read status", tags = "Notification")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    public void updateReadStatus(@RequestBody Set<UUID> notificationIds, @RequestParam boolean readStatus, @Parameter(hidden = true) Principal principal) {
        service.updateReadStatus(notificationIds, readStatus, principal.getName());
    }

    @PostMapping("/delete")
    @SuppressWarnings("squid:S4834")
    @PreAuthorize("hasAuthority('service-user')")
    @Operation(summary = "Delete notification", tags = "Notification")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    public void delete(@RequestBody Set<UUID> notificationIds, @Parameter(hidden = true) Principal principal) {
        service.delete(notificationIds, principal.getName());
    }
}
