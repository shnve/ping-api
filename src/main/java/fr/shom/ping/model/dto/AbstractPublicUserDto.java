package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.enums.UserTitle;
import fr.shom.ping.validation.NoHtml;
import fr.shom.ping.validation.ValidPhoneNumber;
import fr.shom.ping.validation.ValueOfEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "abstract PublicUserDto Model")
public abstract class AbstractPublicUserDto {

    @Schema(description = "Title", required = true)
    @ValueOfEnum(enumClass = UserTitle.class, message = "validation.edit-public-user.title.not-in-enum")
    @NotNull(message = "validation.edit-public-user.title.not-null")
    @NoHtml
    private String title;

    @Schema(description = "Firstname", required = true)
    @NotEmpty(message = "validation.edit-public-user.firstname.not-empty")
    @NoHtml
    private String firstname;

    @Schema(description = "Lastname", required = true)
    @NotEmpty(message = "validation.edit-public-user.lastname.not-empty")
    @NoHtml
    private String lastname;

    @Schema(description = "Email", required = true)
    @NotEmpty(message = "validation.edit-public-user.email.not-empty")
    @Email
    private String email;

    @Schema(description = "Phone number (Should contain country code)", example = "+33101010101")
    @ValidPhoneNumber
    @NoHtml
    private String phoneNumber;

    @Schema(description = "Mobile phone number (Should contain country code)", example = "+33101010101")
    @ValidPhoneNumber
    @NoHtml
    private String mobilePhoneNumber;

    @Schema(description = "Organization")
    @NoHtml
    private String organization;

    @Schema(description = "Ship name")
    @NoHtml
    private String shipName;

    @Schema(description = "Home port")
    @NoHtml
    private String homePort;

    @Schema(description = "Registration number")
    @NoHtml
    private String registrationNumber;

    private boolean proAccount;
}
