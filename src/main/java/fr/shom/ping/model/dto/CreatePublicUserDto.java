package fr.shom.ping.model.dto;

import fr.shom.ping.model.enums.UserTitle;
import fr.shom.ping.validation.AtLeastOnePhoneNumber;
import fr.shom.ping.validation.NoHtml;
import fr.shom.ping.validation.ValidPhoneNumber;
import fr.shom.ping.validation.ValueOfEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.Getter;

@Data
@Builder
@AtLeastOnePhoneNumber
@Schema(name = "CreatePublicUserDto Model")
public class CreatePublicUserDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 7445242417296244868L;

    @Schema(description = "Title", required = true)
    @ValueOfEnum(enumClass = UserTitle.class, message = "validation.create-public-user.title.not-in-enum")
    @NotNull(message = "validation.create-public-user.title.not-null")
    @NoHtml
    private String title;

    @Schema(description = "Firstname", required = true)
    @NotEmpty(message = "validation.create-public-user.firstname.not-empty")
    @NoHtml
    private String firstname;

    @Schema(description = "Lastname", required = true)
    @NotEmpty(message = "validation.create-public-user.lastname.not-empty")
    @NoHtml
    private String lastname;

    @Schema(description = "Email", required = true)
    @NotEmpty(message = "validation.create-public-user.email.not-empty")
    @Email
    private String email;

    @Schema(description = "Password", required = true)
    @NotEmpty(message = "validation.create-public-user.title.not-empty")
    private String password;

    @Schema(description = "Phone number (Should contain country code)", example = "+33101010101")
    @ValidPhoneNumber
    @NoHtml
    private String phoneNumber;

    @Schema(description = "Mobile phone number (Should contain country code)", example = "+33101010101")
    @ValidPhoneNumber
    @NoHtml
    private String mobilePhoneNumber;

    @Schema(description = "Organization")
    private String organization;

    @Schema(description = "Ship name")
    private String shipName;

    @Schema(description = "Home port")
    private String homePort;

    @Schema(description = "Registration number")
    private String registrationNumber;

    @Schema(description = "Preferred area ID")
    private UUID preferredAreaId;

    @Schema(description = "Has email notification (Default value : true)")
    @Default
    @Getter(AccessLevel.NONE)
    private boolean hasEmailNotification = true;

    public boolean hasEmailNotification() {
        return hasEmailNotification;
    }

    @Schema(description = "Has accepted terms of service (Default value : false)", required = true)
    @Default
    @Getter(AccessLevel.NONE)
    @AssertTrue(message = "validation.create-public-user.has-accepted-tos.assert-true")
    private boolean hasAcceptedTos = false;

    public boolean hasAcceptedTos() {
        return hasAcceptedTos;
    }

    @Schema(description = "Has notifications sound (Default value : false)")
    @Default
    @Getter(AccessLevel.NONE)
    private boolean hasNotificationsSound = false;

    public boolean hasNotificationsSound() {
        return hasNotificationsSound;
    }

    @Schema(description = "Has subscription email notifications (Default value : true)")
    @Default
    @Getter(AccessLevel.NONE)
    private boolean hasSubscriptionEmailNotifications = true;

    public boolean hasSubscriptionEmailNotifications() {
        return hasSubscriptionEmailNotifications;
    }

    @Schema(description = "Is pro account (Default value : false)")
    @Default
    private boolean proAccount = false;
}
