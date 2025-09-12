package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.shom.ping.model.enums.WarningHazardTypeGeneral;
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
import jakarta.persistence.ManyToOne;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import static fr.shom.ping.utils.SchemaUtil.SUBSCRIPTION_WARNING_HAZARD_TYPE_GENERAL_TABLE;
import static jakarta.persistence.EnumType.STRING;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@SuperBuilder
@Entity
@DiscriminatorValue("WARNING")
public class WarningSubscription extends Subscription {
    @Serial
    private static final long serialVersionUID = 1042524880946138634L;

    @ManyToOne
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Serie serie;

    private String lang;

    @ManyToMany
    @JoinTable(
        name = "subscription_sub_area",
        joinColumns = @JoinColumn(name = "subscription_id"),
        inverseJoinColumns = @JoinColumn(name = "sub_area_id"))
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @Builder.Default
    private Set<SubArea> subAreas = new HashSet<>();

    @ElementCollection
    @CollectionTable(
        name = SUBSCRIPTION_WARNING_HAZARD_TYPE_GENERAL_TABLE,
        joinColumns = @JoinColumn(name = "subscription_id")
    )
    @Enumerated(STRING)
    @Column(name = "warning_hazard_type_general")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private Set<WarningHazardTypeGeneral> warningHazardTypeGenerals = new HashSet<>();

    @ManyToOne
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JsonIgnore
    private PublicUser publicUser;
}
