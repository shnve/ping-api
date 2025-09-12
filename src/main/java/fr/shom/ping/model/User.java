package fr.shom.ping.model;

import fr.shom.ping.model.enums.UserStatus;
import fr.shom.ping.model.enums.UserTitle;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static fr.shom.ping.model.enums.UserStatus.CREATED;
import static java.time.Instant.now;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.EAGER;

@Data
@NoArgsConstructor
@MappedSuperclass
@SuperBuilder
public abstract class User implements Serializable {
    @Serial
    private static final long serialVersionUID = -8455753359554015704L;

    @Id
    private UUID id;

    @Column(nullable = false)
    protected UUID keycloakId;

    @Column(nullable = false)
    @Enumerated(STRING)
    protected UserTitle title;

    @Column(nullable = false)
    protected String firstname;

    @Column(nullable = false)
    protected String lastname;

    @Column(nullable = false)
    protected String email;

    @Column(nullable = false)
    protected String identifier;

    @Column(nullable = false)
    @Enumerated(STRING)
    @Default
    private UserStatus status = CREATED;

    @Column(nullable = false)
    private Instant creationDate;

    private Instant updateDate;

    @OneToOne(fetch = EAGER)
    private Area preferredArea;

    @Column(nullable = false)
    @Default
    protected boolean hasNotificationsSound = false;

    @Column(nullable = false)
    @Default
    protected boolean hasSubscriptionEmailNotifications = false;

    public boolean hasNotificationsSound() {
        return hasNotificationsSound;
    }

    public boolean hasSubscriptionEmailNotifications() { return hasSubscriptionEmailNotifications; }

    @PrePersist
    public void prePersist() {
        id = ofNullable(id).orElse(randomUUID());
        creationDate = ofNullable(creationDate).orElse(now());
    }
}
