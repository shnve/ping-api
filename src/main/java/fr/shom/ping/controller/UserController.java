package fr.shom.ping.controller;

import fr.shom.ping.exception.BindingException;
import fr.shom.ping.exception.ForbiddenException;
import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.PublicUser;
import fr.shom.ping.model.dto.AuthenticatedPublicUserDto;
import fr.shom.ping.model.dto.CreatePublicUserDto;
import fr.shom.ping.model.dto.EditPublicUserDto;
import fr.shom.ping.model.dto.PublicUserDto;
import fr.shom.ping.model.dto.UpdateEmailDto;
import fr.shom.ping.model.dto.UpdatePasswordDto;
import fr.shom.ping.service.PublicAccountService;
import fr.shom.ping.service.UserService;
import fr.shom.ping.validation.NoHtml;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import java.security.Principal;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequiredArgsConstructor
@Tag(name = "User (Public)", description = "Public User API")
public class UserController {
    private final UserService service;
    private final PublicAccountService accountService;
    private static final ModelMapper MODEL_MAPPER = new ModelMapper();

    @PostMapping("/api/users")
    @RateLimiter(name = "rateLimiterUserCreateAPI")
    @ResponseStatus(CREATED)
    @Operation(summary = "Create public user", tags = "User (Public)")
    public void create(@RequestBody @Valid CreatePublicUserDto user, BindingResult bindingResult) {
        try {
            service.create(user, bindingResult);
        } catch (ServiceException exception) {
            throw new BindingException(bindingResult.getAllErrors());
        }
    }

    @PutMapping("/admin-api/public-users/{id}")
    @PreAuthorize("hasAuthority('functional-admin')")
    @Operation(summary = "Edit public user", tags = "User (Public)")
    public PublicUser edit(@PathVariable UUID id, @RequestBody @Valid EditPublicUserDto dto, BindingResult bindingResult) {
        try {
            return service.edit(id, dto, bindingResult);
        } catch (ServiceException exception) {
            throw new BindingException(bindingResult.getAllErrors());
        }
    }

    @PutMapping("/secured-api/users")
    @Operation(summary = "Edit public user", tags = "User (Public)")
    @PreAuthorize("isAuthenticated()")
    public PublicUser editPublicUser(Principal principal, @RequestBody @Valid EditPublicUserDto dto,
        BindingResult bindingResult) {
        try {
            return service.editPublicUser(principal.getName(), dto, bindingResult);
        } catch (ServiceException exception) {
            throw new BindingException(bindingResult.getAllErrors());
        }
    }

    @PutMapping("/api/users/{id}/email")
    @PreAuthorize("hasAuthority('functional-admin')")
    @Operation(summary = "Edit mail public user", tags = "User (Public)")
    public void editMail(@PathVariable UUID id, @RequestBody @Valid UpdateEmailDto dto) {
        accountService.editMail(id, dto.getEmail());
    }

    @PutMapping("/api/users/{id}/reset-password")
    @PreAuthorize("hasAuthority('functional-admin')")
    @Operation(summary = "Reset password public user", tags = "User (Public)")
    public void resetPassword(@PathVariable UUID id, @RequestBody @Valid UpdatePasswordDto dto) {
        accountService.resetPassword(id, dto.getPassword());
    }

    @GetMapping("/secured-api/users/reset-password")
    @PreAuthorize("isAuthenticated()")
    public void resetPassword(Principal principal) {
        service.sendUpdatePasswordEmail(principal.getName());
    }

    @GetMapping("/secured-api/users/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Find user by id", tags = "User (Public)")
    public PublicUserDto findById(@PathVariable UUID id) {
        return findPublicUserById(id);
    }

    @PostMapping("/secured-api/users/enable")
    @Operation(summary = "Validate public user after email verification", tags = "User (Public)")
    public AuthenticatedPublicUserDto enable(JwtAuthenticationToken authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            throw new ForbiddenException();
        }

        service.enable(authentication.getName());
        return service.me(authentication.getName());
    }

    @GetMapping("/secured-api/users/me")
    @Operation(summary = "Get information of current authenticated user", tags = "User (Public)")
    @PreAuthorize("isAuthenticated()")
    public AuthenticatedPublicUserDto me(Principal principal) {
        return service.me(principal.getName());
    }

    @DeleteMapping("/secured-api/users")
    @ResponseStatus(NO_CONTENT)
    @Operation(summary = "Delete its own public user account", tags = "User (Public)")
    @Parameter(name = "Authorization", description = "Access Token", required = true,  example = "Bearer " +
        "access_token")
    @PreAuthorize("isAuthenticated()")
    public void delete(@Parameter(hidden = true) Principal principal) {
        service.delete(principal.getName());
    }

    @GetMapping("/admin-api/public-users/search")
    @Operation(summary = "Search public user", tags = "User (Public)")
    @PreAuthorize("hasAuthority('functional-admin')")
    public Page<PublicUser> search(Pageable pageable, @RequestParam(required = false) @NoHtml String lastname,
        @RequestParam(required = false) @NoHtml String firstname, @RequestParam(required = false) @NoHtml String email) {
        if (lastname == null && firstname == null && email == null) {
            return service.search(pageable);
        }
        return service.findAllBySearchCriteria(pageable, lastname, firstname, email);
    }

    @GetMapping("/admin-api/public-users/{id}")
    @PreAuthorize("hasAuthority('functional-admin')")
    @Operation(summary = "Find user by id from admin", tags = "User (Public)")
    public PublicUserDto findByIdFromAdmin(@PathVariable UUID id) {
        return findPublicUserById(id);
    }

    private PublicUserDto findPublicUserById(UUID id) {
        return convertToDto(service.findById(id));
    }

    private PublicUserDto convertToDto(PublicUser publicUser) {
        return MODEL_MAPPER.map(publicUser, PublicUserDto.class);
    }
}
