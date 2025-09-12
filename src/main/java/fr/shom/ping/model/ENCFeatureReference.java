package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static java.util.Collections.emptySet;
import static java.util.UUID.randomUUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = NON_NULL)
@Schema(name = "ENCFeatureReference Model")
@Table(name = "enc_feature_reference")
public class ENCFeatureReference implements Serializable {
    @Serial
    private static final long serialVersionUID = -7940446520479303015L;

    @Id
    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @JsonIgnore
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @NotNull(message = "validation.enc-feature-reference.feature-reference.not-null")
    @ManyToOne(optional = false)
    private FeatureReference featureReference;

    @NotEmpty(message = "validation.enc-feature-reference.edition-number.not-null")
    @Column(nullable = false)
    @Schema(description = "Edition number", required = true)
    private String editionNumber;

    @NotEmpty(message = "validation.enc-feature-reference.enc-name.not-empty")
    @Column(nullable = false)
    @Schema(description = "ENC name", required = true)
    private String encName;

    @NotEmpty(message = "validation.enc-feature-reference.feature-object-identifier.set.not-empty")
    @ElementCollection
    @CollectionTable(
        name = "enc_feature_reference_object_identifier",
        joinColumns = @JoinColumn(name = "enc_feature_reference_id")
    )
    @Column(name = "object_identifier", nullable = false)
    @Schema(description = "List of feature object identifier", required = true)
    @EqualsAndHashCode.Exclude
    @Default
    private Set<@NotEmpty(message = "validation.enc-feature-reference.feature-object-identifier.content.not-empty") String> featureObjectIdentifiers = emptySet();

    @NotEmpty(message = "validation.enc-feature-reference.update-number.not-empty")
    @Column(nullable = false)
    @Schema(description = "Update number", required = true)
    private String updateNumber;

    @PrePersist
    public void prePersist() {
        id = randomUUID();
    }
}
