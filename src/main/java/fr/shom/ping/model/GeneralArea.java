package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.dto.GeneralAreaDto;
import fr.shom.ping.validation.ValidURN;
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
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.INTERNATIONAL_NAME_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.NAME_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.URN_DESCRIPTION;
import static fr.shom.ping.utils.SchemaUtil.GENERAL_AREA_TABLE;
import static java.util.UUID.randomUUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = NON_NULL)
@Table(name = GENERAL_AREA_TABLE, uniqueConstraints = @UniqueConstraint(columnNames = {"warning_id", "position"}))
public class GeneralArea implements Serializable, Comparable<GeneralArea> {
    @Serial
    private static final long serialVersionUID = -7297210227112197504L;

    @Id
    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @ManyToOne
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JsonIgnore
    private Warning warning;

    @NotNull(message = "validation.general-area.position.not-null")
    @Min(value = 1, message = "validation.general-area.position.invalid")
    @Column(nullable = false)
    @Default
    private Integer position = 1;

    @NotNull(message = "validation.general-area.sub-area.not-null")
    @OneToOne(optional = false)
    private SubArea subArea;

    @ValidURN(message = "validation.general-area.urn.invalid")
    @Schema(description = URN_DESCRIPTION)
    private String urn;

    @NotEmpty(message = "validation.general-area.name.not-empty")
    @Column(nullable = false)
    @Schema(description = NAME_DESCRIPTION, required = true)
    private String name;

    @Schema(description = INTERNATIONAL_NAME_DESCRIPTION)
    private String internationalName;

    @PrePersist
    public void prePersist() {
        id = randomUUID();
    }

    @Override
    public int compareTo(GeneralArea generalArea) {
        return position.compareTo(generalArea.getPosition());
    }

    public GeneralAreaDto toDto() {
        return GeneralAreaDto.builder()
            .id(subArea.getId())
            .position(position)
            .build();
    }
}
