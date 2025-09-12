package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Area;
import fr.shom.ping.model.enums.UserStatus;
import fr.shom.ping.validation.AtLeastOnePhoneNumber;
import fr.shom.ping.validation.NoHtml;
import fr.shom.ping.validation.ValueOfEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@EqualsAndHashCode(callSuper = true)
@Data
@AtLeastOnePhoneNumber
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "PublicUserDto Model")
public class PublicUserDto extends AbstractPublicUserDto {

    @NoHtml
    private String identifier;

    @ValueOfEnum(enumClass = UserStatus.class, message = "validation.public-user.status.not-in-enum")
    @NotNull(message = "validation.public-user.status.not-null")
    @NoHtml
    private UserStatus status;

    private Instant creationDate;

    private Instant updateDate;

    private Area preferredArea;

    private boolean hasNotificationsSound;

    private boolean hasSubscriptionEmailNotifications;

    private boolean hasEmailNotification;

    private boolean hasAcceptedTos;
}
