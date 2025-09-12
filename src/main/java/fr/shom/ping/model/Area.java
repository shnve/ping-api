package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.dto.AreaCreateDto;
import fr.shom.ping.model.dto.AreaDto;
import fr.shom.ping.validation.ValidCountryCode;
import fr.shom.ping.validation.ValidURN;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.locationtech.jts.geom.Geometry;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.COUNTRY_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.DISCLAIMER;
import static fr.shom.ping.SwaggerConfig.Property.EMPTY_LIST_EXAMPLE;
import static fr.shom.ping.SwaggerConfig.Property.GEOMETRY_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.GEOMETRY_EXAMPLE;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.INFORMATION_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.INTERNATIONAL_DISCLAIMER;
import static fr.shom.ping.SwaggerConfig.Property.INTERNATIONAL_NAME_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.LOCALIZED_INFORMATION_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.NAME_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.URN_DESCRIPTION;
import static java.util.Collections.unmodifiableList;
import static java.util.UUID.randomUUID;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@JsonInclude(value = NON_NULL)
@Schema(name = "Area Model")
public class Area implements Serializable {
    @Serial
    private static final long serialVersionUID = -4374935258735047391L;

    @Id
    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @ValidURN(message = "validation.area.urn.invalid")
    @Schema(description = URN_DESCRIPTION)
    private String urn;

    @NotEmpty(message = "validation.area.name.not-empty")
    @Column(nullable = false)
    @Schema(description = NAME_DESCRIPTION, required = true)
    private String name;

    @Schema(description = INTERNATIONAL_NAME_DESCRIPTION)
    private String internationalName;

    @Schema(description = INFORMATION_DESCRIPTION)
    private String information;

    @Schema(description = LOCALIZED_INFORMATION_DESCRIPTION)
    private String localizedInformation;

    @ValidCountryCode(message = "validation.area.country-code.invalid")
    @Schema(description = COUNTRY_DESCRIPTION)
    private String country;

    @NotNull(message = "validation.area.geometry.not-null")
    @Column(nullable = false)
    @Schema(description = GEOMETRY_DESCRIPTION, required = true, example = GEOMETRY_EXAMPLE)
    private Geometry geom;

    @OneToMany(fetch = LAZY)
    @JoinTable(
        name = "area_border_areas",
        joinColumns = @JoinColumn(name = "border_areas_id"),
        inverseJoinColumns = @JoinColumn(name = "area_id"))
    @Schema(description = "List of border areas", example = EMPTY_LIST_EXAMPLE)
    @JsonBackReference
    private List<Area> borderAreas;

    @Schema(description = DISCLAIMER)
    private String disclaimer;

    @Schema(description = INTERNATIONAL_DISCLAIMER)
    private String internationalDisclaimer;

    @PrePersist
    public void prePersist() {
        id = randomUUID();
    }

    public Area(AreaCreateDto dto, final List<Area> borderAreas) {
        this.country = dto.getCountry();
        this.localizedInformation = dto.getLocalizedInformation();
        this.geom = dto.getGeom();
        this.information = dto.getInformation();
        this.internationalName = dto.getInternationalName();
        this.disclaimer = dto.getDisclaimer();
        this.internationalDisclaimer = dto.getInternationalDisclaimer();
        this.name = dto.getName();
        this.urn = dto.getUrn();
        this.borderAreas = unmodifiableList(borderAreas);
    }

    public AreaDto asDto() {
        return new AreaDto(this);
    }
}
