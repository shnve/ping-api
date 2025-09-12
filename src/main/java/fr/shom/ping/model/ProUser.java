package fr.shom.ping.model;

import fr.shom.ping.model.dto.CreateProUserDto;
import fr.shom.ping.model.dto.PhoneNumberDto;
import fr.shom.ping.model.enums.ProUserRole;
import fr.shom.ping.utils.InfosourceUtil;
import fr.shom.ping.visitor.ProUserConvertVisitor;
import fr.shom.ping.visitor.ProUserEditDtoVisitor;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Inheritance;
import jakarta.persistence.OneToMany;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.InheritanceType.SINGLE_TABLE;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@SuperBuilder
@Entity
@Inheritance(strategy = SINGLE_TABLE)
@DiscriminatorColumn(name = "role", discriminatorType = DiscriminatorType.STRING)
public abstract class ProUser extends User {
    @Serial
    private static final long serialVersionUID = 6367491914283402774L;

    @Column(updatable = false, insertable = false)
    @Enumerated(STRING)
    private ProUserRole role;

    @OneToMany(cascade = ALL, orphanRemoval = true, mappedBy = "user")
    @Default
    private List<PhoneNumber> phoneNumberList = new ArrayList<>();

    public ProUser(ProUser proUser) {
        setId(proUser.getId());
        setStatus(proUser.getStatus());
        setCreationDate(proUser.getCreationDate());
        setUpdateDate(proUser.getUpdateDate());
        setPreferredArea(proUser.getPreferredArea());
        email = proUser.getEmail();
        firstname = proUser.getFirstname();
        keycloakId = proUser.getKeycloakId();
        lastname = proUser.getLastname();
        title = proUser.getTitle();
        phoneNumberList = proUser.getPhoneNumberList();
        hasNotificationsSound = proUser.hasNotificationsSound();
        hasSubscriptionEmailNotifications = proUser.hasSubscriptionEmailNotifications();
        identifier = InfosourceUtil.generateIdentifierFromEmail(proUser.getEmail());
    }

    public ProUser(CreateProUserDto dto, UUID keycloakId) {
        this.email = dto.getEmail();
        this.firstname = dto.getFirstname();
        this.keycloakId = keycloakId;
        this.lastname = dto.getLastname();
        this.title = dto.getTitle();
        this.phoneNumberList = ((List<PhoneNumberDto>) dto.getPhoneNumbers())
            .stream()
            .map(PhoneNumber::new)
            .collect(Collectors.toList());
        this.phoneNumberList.forEach(phoneNumber -> phoneNumber.setUser(this));
        this.hasNotificationsSound = dto.hasNotificationsSound();
        this.hasSubscriptionEmailNotifications = dto.hasSubscriptionEmailNotifications();
        this.identifier = InfosourceUtil.generateIdentifierFromEmail(dto.getEmail());
    }

    public abstract <T> T toEditDto(ProUserEditDtoVisitor<T> visitor);

    public abstract <T> T switchRole(ProUserConvertVisitor<T> visitor);
}
