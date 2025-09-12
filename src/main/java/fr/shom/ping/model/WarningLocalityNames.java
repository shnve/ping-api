package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.dto.WarningLocalityNamesDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static fr.shom.ping.utils.SchemaUtil.WARNING_LOCALITY_NAMES_TABLE;
import static java.util.UUID.randomUUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = NON_NULL)
@Table(name = WARNING_LOCALITY_NAMES_TABLE, uniqueConstraints = @UniqueConstraint(columnNames = {"warning_id", "position"}))
public class WarningLocalityNames implements Serializable, Comparable<WarningLocalityNames> {

    @Serial
    private static final long serialVersionUID = 3140660300271545772L;

    @Id
    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @ManyToOne
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JsonIgnore
    private Warning warning;

    @NotNull(message = "validation.warning-locality-names.position.not-null")
    @Min(value = 1, message = "validation.warning-locality-names.position.invalid")
    @Column(nullable = false)
    @Builder.Default
    private Integer position = 1;

    @NotNull(message = "validation.warning-locality-names.parent.not-null")
    @OneToOne(optional = false)
    private SubArea parent;

    @NotEmpty(message = "validation.warning-locality-names.name.not-empty")
    @Column(nullable = false)
    @Schema(description = "Name of locality", required = true)
    private String name;

    @NotNull(message = "validation.warning-locality-names.international-name.not-null")
    @Column(nullable = false)
    @Schema(description = "International name of locality", required = true)
    private String internationalName;

    @PrePersist
    public void prePersist() {
        id = randomUUID();
    }

    @Override
    public int compareTo(WarningLocalityNames locality) {
        return position.compareTo(locality.getPosition());
    }

    public WarningLocalityNamesDto toDto() {
        return WarningLocalityNamesDto.builder()
            .id(id)
            .name(name)
            .internationalName(internationalName)
            .position(position)
            .parentId(parent.getId())
            .build();
    }
}
