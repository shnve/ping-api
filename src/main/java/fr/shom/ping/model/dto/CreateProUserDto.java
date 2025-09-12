package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import fr.shom.ping.model.ProUser;
import fr.shom.ping.model.enums.ProUserRole;
import fr.shom.ping.model.enums.UserTitle;
import fr.shom.ping.validation.NoHtml;
import fr.shom.ping.visitor.ProUserVisitor;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    visible = true,
    property = "role")
@JsonSubTypes({
    @JsonSubTypes.Type(value = CreateServiceUserDto.class, name = "SERVICE_USER"),
    @JsonSubTypes.Type(value = CreateFunctionalAdminDto.class, name = "FUNCTIONAL_ADMIN"),
})
@Data
@SuperBuilder
@NoArgsConstructor
@Schema(name = "CreateProUserDto Model")
public abstract class CreateProUserDto<T extends ProUser> implements Serializable {
    @Serial
    private static final long serialVersionUID = 8792119172500000854L;

    @Schema(description = "Title", required = true)
    @NotNull(message = "validation.create-pro-user.title.not-null")
    private UserTitle title;

    @Schema(description = "Firstname", required = true)
    @NotEmpty(message = "validation.create-pro-user.firstname.not-empty")
    @NoHtml
    private String firstname;

    @Schema(description = "Lastname", required = true)
    @NotEmpty(message = "validation.create-pro-user.lastname.not-empty")
    @NoHtml
    private String lastname;

    @Schema(description = "Email", required = true)
    @NotEmpty(message = "validation.create-pro-user.email.not-empty")
    @Email
    private String email;

    @Schema(description = "Preferred area ID")
    private UUID preferredAreaId;

    @Schema(description = "role", required = true)
    @JsonProperty(value = "role", access = WRITE_ONLY)
    private ProUserRole role;

    @Schema(description = "Phone numbers")
    @Size(min = 1, message = "validation.create-pro-user.phones.size")
    private List<@Valid PhoneNumberDto> phoneNumbers;

    @Schema(description = "Has notifications sound (Default value : false)")
    @Builder.Default
    @Getter(AccessLevel.NONE)
    private boolean hasNotificationsSound = false;

    public boolean hasNotificationsSound() {
        return hasNotificationsSound;
    }

    @Schema(description = "Has subscription email notifications (Default value : true)")
    @Builder.Default
    @Getter(AccessLevel.NONE)
    private boolean hasSubscriptionEmailNotifications = true;

    public boolean hasSubscriptionEmailNotifications() {
        return hasSubscriptionEmailNotifications;
    }

    public abstract T toModel(ProUserVisitor visitor, UUID keycloakId);

    public abstract T update(ProUserVisitor visitor, T proUser);
}
