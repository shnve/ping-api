package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.dto.MaritimeEntityCreateDto;
import fr.shom.ping.model.dto.MaritimeEntityDto;
import fr.shom.ping.model.enums.MaritimeEntityType;
import fr.shom.ping.validation.ValidURN;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.locationtech.jts.geom.Geometry;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.EMPTY_LIST_EXAMPLE;
import static fr.shom.ping.SwaggerConfig.Property.GEOMETRY_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.GEOMETRY_EXAMPLE;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.INFORMATION_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.INTERNATIONAL_NAME_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.LOCALIZED_INFORMATION_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.NAME_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.URN_DESCRIPTION;
import static java.util.UUID.randomUUID;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.EAGER;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = NON_NULL)
@Schema(name = "Maritime Entity Model")
public class MaritimeEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 2794902429202035139L;

    @Id
    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @ValidURN(message = "validation.maritime-entity.urn.invalid")
    @Schema(description = URN_DESCRIPTION)
    private String urn;

    @NotEmpty(message = "validation.maritime-entity.name.not-empty")
    @Column(nullable = false)
    @Schema(description = NAME_DESCRIPTION, required = true)
    private String name;

    @Schema(description = INTERNATIONAL_NAME_DESCRIPTION)
    private String internationalName;

    @Schema(description = INFORMATION_DESCRIPTION)
    private String information;

    @Schema(description = LOCALIZED_INFORMATION_DESCRIPTION)
    private String localizedInformation;

    @NotNull(message = "validation.maritime-entity.type.not-null")
    @Column(nullable = false)
    @Enumerated(STRING)
    @Schema(description = "Maritime Entity Type", required = true)
    private MaritimeEntityType type;

    @NotNull(message = "validation.maritime-entity.geometry.not-null")
    @Column(nullable = false, columnDefinition = "Geometry")
    @Schema(description = GEOMETRY_DESCRIPTION, required = true, example = GEOMETRY_EXAMPLE)
    private Geometry geom;

    @OneToMany
    @JoinTable(
        name = "maritime_entity_areas",
        joinColumns = @JoinColumn(name = "maritime_entity_id"),
        inverseJoinColumns = @JoinColumn(name = "areas_id"))
    @Schema(description = "List of linked areas", example = EMPTY_LIST_EXAMPLE)
    private List<Area> areas;

    @ManyToOne
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JsonIgnore
    private MaritimeService maritimeService;

    @Column(unique = true)
    private String sissyId;

    @OneToMany(mappedBy = "maritimeEntity", fetch = EAGER)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private Set<MaritimeEntityReference> references = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "maritime_entity_additional_service",
        joinColumns = @JoinColumn(name = "maritime_entity_id"),
        inverseJoinColumns = @JoinColumn(name = "maritime_service_id"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private Set<MaritimeService> additionalServices = new HashSet<>();

    @PrePersist
    public void prePersist() {
        id = randomUUID();
    }

    public MaritimeEntity(MaritimeEntityCreateDto dto, final List<Area> areas, final MaritimeService maritimeService,
        final Set<MaritimeService> additionalServices) {
        this.type = dto.getType();
        this.localizedInformation = dto.getLocalizedInformation();
        this.geom = dto.getGeom();
        this.information = dto.getInformation();
        this.internationalName = dto.getInternationalName();
        this.name = dto.getName();
        this.urn = dto.getUrn();
        this.areas = areas;
        this.maritimeService = maritimeService;
        this.sissyId = dto.getSissyId();
        this.additionalServices = additionalServices;
    }

    public MaritimeEntityDto asDto() {
        return new MaritimeEntityDto(this);
    }
}
