package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.enums.WarningHazardTypeDetails;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.Set;
import java.util.SortedSet;
import java.util.UUID;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newTreeSet;
import static fr.shom.ping.SwaggerConfig.Property.GEOMETRY_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.GEOMETRY_EXAMPLE;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static fr.shom.ping.utils.SchemaUtil.AFFECTED_AREA_TABLE;
import static fr.shom.ping.utils.SchemaUtil.FEATURE_PART_TABLE;
import static fr.shom.ping.utils.SchemaUtil.WARNING_FEATURE_PART_GEOMETRY;
import static fr.shom.ping.utils.SchemaUtil.WARNING_HAZARD_TYPE_DETAILS_TABLE;
import static java.util.UUID.randomUUID;
import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.EAGER;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = NON_NULL)
@Schema(name = "NWFeaturePart Model")
@Table(name = FEATURE_PART_TABLE, uniqueConstraints = @UniqueConstraint(columnNames = {"warning_id", "position"}))
public class FeaturePart implements Serializable, Comparable<FeaturePart> {
    @Serial
    private static final long serialVersionUID = 3175392409098432549L;

    @Id
    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @NotNull(message = "validation.feature-part.warning.not-null")
    @ManyToOne(optional = false)
    private Warning warning;

    @NotNull(message = "validation.feature-part.position.not-null")
    @Min(value = 1, message = "validation.feature-part.position.invalid")
    @Column(nullable = false)
    @Default
    private Integer position = 1;

    @ElementCollection(fetch = EAGER)
    @CollectionTable(
        name = WARNING_FEATURE_PART_GEOMETRY,
        joinColumns = @JoinColumn(name = "feature_part_id")
    )
    @Schema(description = GEOMETRY_DESCRIPTION, example = GEOMETRY_EXAMPLE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Default
    private Set<Feature> geometries = newHashSet();

    @ElementCollection(fetch = EAGER)
    @CollectionTable(
        name = AFFECTED_AREA_TABLE,
        joinColumns = @JoinColumn(name = "feature_part_id")
    )
    @Schema(description = GEOMETRY_DESCRIPTION, example = GEOMETRY_EXAMPLE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Default
    private Set<Feature> affectedAreas = newHashSet();

    @OneToMany(cascade = ALL)
    @JoinTable(
        name = "warning_feature_part_feature_reference",
        joinColumns = @JoinColumn(name = "feature_part_id"),
        inverseJoinColumns = @JoinColumn(name = "feature_reference_id"))
    @Schema(description = "List of feature reference")
    @EqualsAndHashCode.Exclude
    @Default
    private Set<FeatureReference> featureReferences = newHashSet();

    @ElementCollection
    @CollectionTable(
        name = "fixed_date_range",
        joinColumns = @JoinColumn(name = "parent_id")
    )
    @Schema(description = "List of fixed date range. Sorted")
    @EqualsAndHashCode.Exclude
    @Default
    private SortedSet<FixedDateRange> fixedDateRanges = newTreeSet();

    // Begin Warning Information
    @JsonIgnore
    @ElementCollection
    @CollectionTable(
        name = "localized_text",
        joinColumns = @JoinColumn(name = "parent_id")
    )
    @EqualsAndHashCode.Exclude
    @Default
    private Set<LocalizedText> information = newHashSet();

    @ElementCollection
    @CollectionTable(
        name = WARNING_HAZARD_TYPE_DETAILS_TABLE,
        joinColumns = @JoinColumn(name = "feature_part_id")
    )
    @Enumerated(STRING)
    @Column(name = "warning_hazard_type_detail")
    @Schema(description = "Warning hazard type details")
    @EqualsAndHashCode.Exclude
    @Default
    private Set<WarningHazardTypeDetails> warningHazardTypeDetails = newHashSet();
    // End Warning Information

    @PrePersist
    public void prePersist() {
        id = randomUUID();
    }

    @Override
    public int compareTo(FeaturePart featurePart) {
        return position.compareTo(featurePart.getPosition());
    }
}
