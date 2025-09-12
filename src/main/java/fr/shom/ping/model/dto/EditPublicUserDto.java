package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.validation.AtLeastOnePhoneNumber;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
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
@Schema(name = "EditPublicUserDto Model")
public class EditPublicUserDto extends AbstractPublicUserDto {

    @Schema(description = "Preferred area ID")
    private UUID preferredAreaId;

    @Schema(description = "Has notifications sound")
    @Getter(AccessLevel.NONE)
    private boolean hasNotificationsSound;

    public boolean hasNotificationsSound() {
        return hasNotificationsSound;
    }

    @Schema(description = "Has subscription email notifications")
    @Getter(AccessLevel.NONE)
    private boolean hasSubscriptionEmailNotifications;

    public boolean hasSubscriptionEmailNotifications() {
        return hasSubscriptionEmailNotifications;
    }
}
