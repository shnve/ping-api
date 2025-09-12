package fr.shom.ping.model;

import fr.shom.ping.model.enums.GeoregulationType;
import java.io.Serial;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import static jakarta.persistence.EnumType.STRING;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@SuperBuilder
@Entity
@DiscriminatorValue("GEOREGULATION")
public class GeoregulationSubscription extends Subscription {
    @Serial
    private static final long serialVersionUID = 1366172217381741763L;

    @ElementCollection
    @Enumerated(STRING)
    @CollectionTable(name = "subscription_georegulation_type",
        joinColumns = @JoinColumn(name = "subscription_id"))
    @Column(name = "georegulation_type")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private Set<GeoregulationType> georegulationTypes = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "subscription_maritime_entity",
        joinColumns = @JoinColumn(name = "subscription_id"),
        inverseJoinColumns = @JoinColumn(name = "maritime_entity_id"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private Set<MaritimeEntity> maritimeEntities = new HashSet<>();
}
