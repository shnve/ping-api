package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.dto.SubAreaCreateDto;
import fr.shom.ping.model.dto.SubAreaDetailsDto;
import fr.shom.ping.validation.ValidURN;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.locationtech.jts.geom.Geometry;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.GEOMETRY_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.GEOMETRY_EXAMPLE;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.INTERNATIONAL_NAME_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.NAME_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.TECHNICAL_IDENTIFIER;
import static fr.shom.ping.SwaggerConfig.Property.URN_DESCRIPTION;
import static fr.shom.ping.utils.SchemaUtil.SUB_AREA_TABLE;
import static jakarta.persistence.FetchType.LAZY;
import static java.util.UUID.randomUUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = NON_NULL)
@Table(name = SUB_AREA_TABLE)
public class SubArea implements Serializable {
    @Serial
    private static final long serialVersionUID = 6752892655529037534L;

    @Id
    @Schema(description = ID_DESCRIPTION, requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID id;

    @ValidURN(message = "validation.sub-area.urn.invalid")
    @Schema(description = URN_DESCRIPTION)
    private String urn;

    @Column
    @Schema(description = TECHNICAL_IDENTIFIER)
    private String technicalIdentifier;

    @NotEmpty(message = "validation.sub-area.name.not-empty")
    @Column(nullable = false)
    @Schema(description = NAME_DESCRIPTION, requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = INTERNATIONAL_NAME_DESCRIPTION)
    private String internationalName;

    @Schema(description = GEOMETRY_DESCRIPTION, example = GEOMETRY_EXAMPLE)
    @Column(columnDefinition = "Geometry")
    private Geometry geom;

    @JoinTable(name = "sub_area_relationship", joinColumns = @JoinColumn(name = "parent_id"), inverseJoinColumns = @JoinColumn(name = "sub_areas_id"))
    @OneToMany(fetch = LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Default
    private Set<SubArea> subAreas = new HashSet<>();

    @JoinTable(name = "sub_area_relationship", joinColumns = @JoinColumn(name = "sub_areas_id"), inverseJoinColumns = @JoinColumn(name = "parent_id"))
    @OneToMany(fetch = LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Default
    @JsonIgnore
    private Set<SubArea> parents = new HashSet<>();

    @JoinTable(name = "serie_sub_area", joinColumns = @JoinColumn(name = "sub_area_id"), inverseJoinColumns = @JoinColumn(name = "serie_id"))
    @OneToMany(fetch = LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Default
    @JsonIgnore
    private Set<Serie> series = new HashSet<>();

    @ManyToOne
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JsonIgnore
    private Area area;

    public SubArea(SubAreaCreateDto dto, Set<SubArea> childSubAreas, Area area) {
        this.geom = dto.getGeom();
        this.internationalName = dto.getInternationalName();
        this.technicalIdentifier = dto.getTechnicalIdentifier();
        this.name = dto.getName();
        this.urn = dto.getUrn();
        this.subAreas = Collections.unmodifiableSet(childSubAreas);
        this.area = area;
    }

    @PrePersist
    public void prePersist() {
        id = randomUUID();
    }

    public SubAreaDetailsDto asDto() {
        return new SubAreaDetailsDto(this);
    }

    public Set<UUID> getMaritimeServiceIds() {
        Set<Serie> areaSeries = new HashSet<>();
        if (this.getParents().isEmpty()) {
            areaSeries.addAll(this.getSeries());
        } else {
            this.getParents().forEach(parent -> areaSeries.addAll(parent.getSeries()));
        }
        return areaSeries.stream().map(areaSerie -> areaSerie.getMaritimeService().getId()).collect(Collectors.toSet());
    }
}
