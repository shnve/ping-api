package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Area;
import fr.shom.ping.model.ProUser;
import fr.shom.ping.model.enums.ProUserRole;
import fr.shom.ping.model.enums.UserStatus;
import fr.shom.ping.model.enums.UserTitle;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

@Data
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "ProUser Edit DTO Model")
public class ProUserEditDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -6331866873106149303L;

    private UUID id;
    private ProUserRole role;
    private UserTitle title;

    @NoHtml
    private String firstname;

    @NoHtml
    private String lastname;

    @Email
    private String email;

    @NoHtml
    private String identifier;
    private UUID preferredAreaId;
    private List<@Valid PhoneNumberDto> phoneNumbers;
    private boolean hasNotificationsSound;
    private boolean hasSubscriptionEmailNotifications;
    private UserStatus status;

    public ProUserEditDto(ProUser proUser) {
        id = proUser.getId();
        role = proUser.getRole();
        title = proUser.getTitle();
        firstname = proUser.getFirstname();
        lastname = proUser.getLastname();
        email = proUser.getEmail();
        identifier = proUser.getIdentifier();
        preferredAreaId = ofNullable(proUser.getPreferredArea()).map(Area::getId).orElse(null);
        phoneNumbers = proUser.getPhoneNumberList().stream().map(PhoneNumberDto::new).collect(toList());
        hasNotificationsSound = proUser.hasNotificationsSound();
        hasSubscriptionEmailNotifications = proUser.hasSubscriptionEmailNotifications();
        status = proUser.getStatus();
    }
}
