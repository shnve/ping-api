package fr.shom.ping.controller;

import fr.shom.ping.exception.BindingException;
import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.dto.AuthenticatedProUserDto;
import fr.shom.ping.model.dto.CreateProUserDto;
import fr.shom.ping.model.dto.ProUserEditDto;
import fr.shom.ping.model.dto.ProUserSearchDto;
import fr.shom.ping.service.ProUserService;
import fr.shom.ping.service.UserService;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/admin-api/users")
@RequiredArgsConstructor
@Tag(name = "User (Pro)", description = "Pro User API")
@Slf4j
public class ProUserController {

    private final ProUserService service;

    private final UserService publicUserService;

    @GetMapping("/me")
    @Operation(summary = "Get information of current authenticated user", tags = "User (Pro)")
    @PreAuthorize("isAuthenticated()")
    public AuthenticatedProUserDto me(Principal principal) throws ServiceException {
        try {
            return service.me(principal.getName());
        } catch (NotFoundException e) {
            throw new ServiceException("Admin user email '" + principal.getName() + "' not found");
        }
    }

    @GetMapping("/search")
    @Operation(summary = "Get the list of pro user by page", tags = "User (Pro)")
    @PreAuthorize("hasAuthority('functional-admin') or hasAuthority('service-user')")
    public Page<ProUserSearchDto> search(Pageable pageable, @RequestParam(required = false) @NoHtml String lastname,
        @RequestParam(required = false) @NoHtml String firstname, @RequestParam(required = false) @NoHtml String email,
        @Parameter(hidden = true) Principal principal) {
        if (lastname == null && firstname == null && email == null) {
            return service.search(pageable, principal.getName());
        }
        return service.findAllBySearchCriteria(pageable, lastname, firstname, email, principal.getName());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('functional-admin') or hasAuthority('service-user')")
    public ProUserEditDto findById(@PathVariable UUID id) {
        return this.service.findById(id);
    }

    @PostMapping
    @ResponseStatus(CREATED)
    @Operation(summary = "Create pro user", tags = "User (Pro)")
    @SuppressWarnings("squid:S4834")
    @PreAuthorize("hasAuthority('functional-admin') OR" +
        " (#user.role == T(fr.shom.ping.model.enums.ProUserRole).SERVICE_USER)" +
        " AND hasAuthority('service-user') and hasPermission(#user.serviceId, 'SERVICE_ADMIN')")
    public void create(@RequestBody @Valid CreateProUserDto user, BindingResult bindingResult) {
        try {
            service.create(user, bindingResult);
        } catch (ServiceException exception) {
            throw new BindingException(bindingResult.getAllErrors());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update pro user", tags = "User (Pro)")
    @SuppressWarnings("squid:S4834")
    @PreAuthorize("hasAuthority('functional-admin') OR hasAuthority('service-user')")
    public void update(@PathVariable UUID id, @RequestBody @Valid CreateProUserDto user, Principal principal,
        BindingResult bindingResult) {
        try {
            service.update(id, user, principal.getName(), bindingResult);
        } catch (ServiceException exception) {
            throw new BindingException(bindingResult.getAllErrors());
        }
    }

    @GetMapping("/reset-password")
    @PreAuthorize("hasAuthority('functional-admin') or hasAuthority('service-user')")
    public void resetPassword(Principal principal) {
        service.resetPassword(principal.getName());
    }

    @PutMapping("{id}/disable")
    @SuppressWarnings("squid:S4834")
    @Operation(summary = "Disable pro user", tags = "User (Pro)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    @PreAuthorize("hasAuthority('functional-admin')")
    public void disable(@PathVariable UUID id) {
        service.disable(id);
    }

    @PutMapping("{id}/enable")
    @SuppressWarnings("squid:S4834")
    @Operation(summary = "enable pro user", tags = "User (Pro)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    @PreAuthorize("hasAuthority('functional-admin')")
    public void enable(@PathVariable UUID id) {
        service.enable(id);
    }

    @PutMapping("/public/{id}/enable")
    @SuppressWarnings("squid:S4834")
    @Operation(summary = "Enable public user", tags = "User (Public)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    @PreAuthorize("hasAuthority('functional-admin')")
    public void enablePublicUser(@PathVariable UUID id) {
        publicUserService.enable(id);
    }

    @PutMapping("/public/{id}/disable")
    @SuppressWarnings("squid:S4834")
    @Operation(summary = "Disable public user", tags = "User (Public)")
    @Parameter(name = "Authorization", description = "Access Token", required = true, example = "Bearer " +
        "access_token")
    @PreAuthorize("hasAuthority('functional-admin')")
    public void disablePublicUser(@PathVariable UUID id) {
        publicUserService.disable(id);
    }
}
