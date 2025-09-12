package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Type;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.google.common.collect.Sets.newHashSet;
import static fr.shom.ping.SwaggerConfig.Property.GEOMETRY_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.GEOMETRY_EXAMPLE;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static fr.shom.ping.utils.SchemaUtil.INFOSOURCE_GEOMETRY_TABLE;
import static fr.shom.ping.utils.SchemaUtil.INFOSOURCE_PART_TABLE;
import static java.util.UUID.randomUUID;
import static jakarta.persistence.FetchType.EAGER;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = NON_NULL)
@Schema(name = "Infosource Part Model")
@Table(name = INFOSOURCE_PART_TABLE, uniqueConstraints = @UniqueConstraint(columnNames = {"infosource_id", "position"}))
public class InfosourcePart implements Serializable, Comparable<InfosourcePart> {
    @Serial
    private static final long serialVersionUID = 1336267898790450349L;

    @Id
    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @NotNull(message = "validation.infosource-part.infosource.not-null")
    @ManyToOne(optional = false)
    private Infosource infosource;

    @NotNull(message = "validation.infosource-part.position.not-null")
    @Min(value = 1, message = "validation.infosource-part.position.invalid")
    @Column(nullable = false)
    @Default
    private Integer position = 1;

    @NotEmpty(message = "validation.infosource-part.description.not-empty")
    @Column(nullable = false)
    private String description;

    private String internationalDescription;

    @ElementCollection(fetch = EAGER)
    @CollectionTable(
        name = INFOSOURCE_GEOMETRY_TABLE,
        joinColumns = @JoinColumn(name = "feature_part_id")
    )
    @Schema(description = GEOMETRY_DESCRIPTION, example = GEOMETRY_EXAMPLE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Default
    private Set<Feature> localisation = newHashSet();

    @Type(JsonType.class)
    @Column(columnDefinition = "json")
    private Map<String, Object> object;

    @PrePersist
    public void prePersist() {
        id = randomUUID();
    }

    @Override
    public int compareTo(InfosourcePart infosourcePart) {
        return position.compareTo(infosourcePart.getPosition());
    }
}
