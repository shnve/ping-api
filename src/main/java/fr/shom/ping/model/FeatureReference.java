package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.validation.ValidURN;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.Set;
import java.util.UUID;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static java.util.Collections.emptySet;
import static java.util.UUID.randomUUID;
import static jakarta.persistence.CascadeType.ALL;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = NON_NULL)
@Schema(name = "FeatureReference Model")
public class FeatureReference implements Serializable {
    @Serial
    private static final long serialVersionUID = 3893722739980237423L;

    @Id
    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @OneToMany(mappedBy = "featureReference", cascade = ALL)
    @Schema(description = "List of ENC feature reference")
    @EqualsAndHashCode.Exclude
    @Default
    private Set<ENCFeatureReference> encFeatureReferences = emptySet();

    @ElementCollection
    @CollectionTable(
        name = "feature_reference_identifier",
        joinColumns = @JoinColumn(name = "feature_reference_id")
    )
    @Column(name = "identifier")
    @Schema(description = "List of feature identifier (URN)")
    @EqualsAndHashCode.Exclude
    @Default
    private Set<@ValidURN(message = "validation.feature-reference.feature-identifiers.invalid") String> featureIdentifiers = emptySet();

    @ElementCollection
    @CollectionTable(
        name = "feature_reference_light_number",
        joinColumns = @JoinColumn(name = "feature_reference_id")
    )
    @Column(name = "light_number")
    @Schema(description = "List of light number")
    @EqualsAndHashCode.Exclude
    @Default
    private Set<String> lightNumbers = emptySet();

    @PrePersist
    public void prePersist() {
        id = randomUUID();
        encFeatureReferences.forEach(encFeatureReference -> encFeatureReference.setFeatureReference(this));
    }
}
