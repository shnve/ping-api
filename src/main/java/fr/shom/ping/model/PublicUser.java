package fr.shom.ping.model;

import fr.shom.ping.model.dto.CreatePublicUserDto;
import fr.shom.ping.model.enums.UserTitle;
import java.io.Serial;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import fr.shom.ping.utils.InfosourceUtil;
import lombok.AccessLevel;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
@Entity
public class PublicUser extends User {
    @Serial
    private static final long serialVersionUID = 1012840996196221071L;

    private String phoneNumber;
    private String mobilePhoneNumber;
    private String organization;
    private String shipName;
    private String homePort;
    private String registrationNumber;

    @Getter(AccessLevel.NONE)
    @Default
    private boolean hasEmailNotification = true;

    public boolean hasEmailNotification() {
        return hasEmailNotification;
    }

    @Getter(AccessLevel.NONE)
    @Default
    private boolean hasAcceptedTos = false;

    public boolean hasAcceptedTos() {
        return hasAcceptedTos;
    }

    @Default
    @Column(name = "is_pro_account")
    private boolean proAccount = false;

    public static PublicUser fromCreatePublicUserDto(CreatePublicUserDto user, UUID keycloakId, String identifier) {
        return PublicUser.builder()
            .keycloakId(keycloakId)
            .title(UserTitle.valueOf(user.getTitle()))
            .firstname(user.getFirstname())
            .lastname(user.getLastname())
            .email(user.getEmail())
            .phoneNumber(user.getPhoneNumber())
            .mobilePhoneNumber(user.getMobilePhoneNumber())
            .organization(user.getOrganization())
            .shipName(user.getShipName())
            .homePort(user.getHomePort())
            .registrationNumber(user.getRegistrationNumber())
            .hasEmailNotification(user.hasEmailNotification())
            .hasAcceptedTos(user.hasAcceptedTos())
            .identifier(identifier)
            .hasNotificationsSound(user.hasNotificationsSound())
            .hasSubscriptionEmailNotifications(user.hasSubscriptionEmailNotifications())
            .proAccount(user.isProAccount())
            .build();
    }
}
