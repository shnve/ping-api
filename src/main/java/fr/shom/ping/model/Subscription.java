package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.shom.ping.model.enums.SubscriptionType;
import fr.shom.ping.model.tuple.SubscriptionTableTuple;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.SqlResultSetMappings;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import static fr.shom.ping.utils.SchemaUtil.SUBSCRIPTION_TABLE_VIEW_TUPLE_MAPPING;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.InheritanceType.SINGLE_TABLE;

@Data
@NoArgsConstructor
@SuperBuilder
@Entity
@Inheritance(strategy = SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
@SqlResultSetMappings(value = {
    @SqlResultSetMapping(name = SUBSCRIPTION_TABLE_VIEW_TUPLE_MAPPING,
        classes = {
            @ConstructorResult(targetClass = SubscriptionTableTuple.class,
                columns = {
                    @ColumnResult(name = "id", type = UUID.class),
                    @ColumnResult(name = "title", type = String.class),
                    @ColumnResult(name = "creation_date", type = Instant.class),
                    @ColumnResult(name = "sending_mail", type = Boolean.class),
                    @ColumnResult(name = "area_name", type = String.class),
                    @ColumnResult(name = "area_international_name", type = String.class),
                    @ColumnResult(name = "maritime_service_subscriber_name", type = String.class),
                    @ColumnResult(name = "maritime_service_subscriber_international_name", type = String.class),
                    @ColumnResult(name = "type", type = String.class)
                }
            )
        }
    )
})
public abstract class Subscription implements Serializable {
    @Serial
    private static final long serialVersionUID = 1670801685142789303L;

    @Id
    private UUID id;

    @Column(nullable = false)
    private Instant creationDate;

    @NotNull(message = "validation.subscription.title.not-null")
    @Column(nullable = false)
    private String title;

    @Enumerated(STRING)
    @Column(insertable = false, updatable = false)
    private SubscriptionType type;

    @Column(nullable = false)
    @Builder.Default
    private boolean sendingMail = true;

    @ManyToOne
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JsonIgnore
    private MaritimeService service;

    @NotNull(message = "validation.subscription.area.not-null")
    @ManyToOne
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JsonIgnore
    private Area area;

    @ManyToMany
    @JoinTable(
        name = "subscription_maritime_entity_geographic_extent",
        joinColumns = @JoinColumn(name = "subscription_id"),
        inverseJoinColumns = @JoinColumn(name = "maritime_entity_id"))
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @Builder.Default
    private Set<MaritimeEntity> maritimeEntitiesGeographicExtent = new HashSet<>();

    @OneToMany(mappedBy = "subscription", fetch = LAZY, cascade = ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private Set<Notification> notifications = new HashSet<>();

    @PrePersist
    public void prePersist() {
        id = randomUUID();
        creationDate = now();
    }
}
