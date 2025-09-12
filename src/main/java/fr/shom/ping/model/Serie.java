package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.dto.SerieCreateDto;
import fr.shom.ping.model.dto.SerieDetailsDto;
import fr.shom.ping.model.enums.DiffusionNavtexSource;
import fr.shom.ping.model.enums.SerieType;
import fr.shom.ping.validation.ValidLanguage;
import fr.shom.ping.validation.ValidURN;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import static fr.shom.ping.SwaggerConfig.Property.INFORMATION_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.INTERNATIONAL_NAME_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.LOCALIZED_INFORMATION_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.NAME_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.URN_DESCRIPTION;
import static fr.shom.ping.utils.SchemaUtil.SERIE_TABLE;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.EAGER;
import static java.util.Collections.emptySet;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toMap;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = NON_NULL)
@Schema(name = "Serie Model")
@Table(name = SERIE_TABLE)
public class Serie implements Serializable {
    @Serial
    private static final long serialVersionUID = 6401067402867821653L;

    @Id
    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @ValidURN(message = "validation.serie.urn.invalid")
    @Schema(description = URN_DESCRIPTION)
    private String urn;

    @NotEmpty(message = "validation.serie.name.not-empty")
    @Column(nullable = false)
    @Schema(description = NAME_DESCRIPTION, required = true)
    private String name;

    @Schema(description = INTERNATIONAL_NAME_DESCRIPTION)
    private String internationalName;

    @Schema(description = INFORMATION_DESCRIPTION)
    private String information;

    @Schema(description = LOCALIZED_INFORMATION_DESCRIPTION)
    private String localizedInformation;

    @NotNull(message = "validation.serie.type.not-null")
    @Column(nullable = false)
    @Enumerated(STRING)
    @Schema(description = "Serie Type", required = true)
    private SerieType type;

    @ValidLanguage(message = "validation.serie.mandatory-languages.invalid")
    @Size(min = 1, max = 2, message = "validation.serie.mandatory-languages.size")
    @ElementCollection(fetch = EAGER)
    @Column(nullable = false)
    @Schema(description = "List of mandatory languages", required = true, example = "[ \"fr\" ]")
    @EqualsAndHashCode.Exclude
    @Default
    private Set<String> mandatoryLanguages = emptySet();

    @Size(max = 2, message = "validation.serie.optional-languages.max-size")
    @ElementCollection(fetch = EAGER)
    @Schema(description = "List of optional languages", example = "[ \"en\" ]")
    @EqualsAndHashCode.Exclude
    @Default
    private Set<@ValidLanguage(message = "validation.serie.optional-languages.invalid") String> optionalLanguages = emptySet();

    @Schema(description = "Description")
    private String description;

    @NotNull(message = "validation.serie.maritime-service.not-null")
    @OneToOne(optional = false)
    @Schema(description = "Maritime Service", required = true)
    @ToString.Exclude
    private MaritimeService maritimeService;

    @ElementCollection
    @CollectionTable(
        name = "serie_ranges",
        joinColumns = @JoinColumn(name = "serie_id")
    )
    @MapKeyColumn(name = "beginning")
    @Column(name = "ending")
    @Default
    private Map<Integer, Integer> ranges = new HashMap<>();

    @ManyToMany
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JoinTable(
        name = "serie_sub_area",
        joinColumns = @JoinColumn(name = "serie_id"),
        inverseJoinColumns = @JoinColumn(name = "sub_area_id"))
    @Default
    private List<SubArea> subAreas = new ArrayList<>();

    @Enumerated(STRING)
    @Schema(description = "navtex source")
    private DiffusionNavtexSource navtexSource;

    @Schema(description = "S124 Identifier")
    @Column(name = "s124_identifier")
    private String s124Identifier;

    public Serie(SerieCreateDto dto, MaritimeService maritimeService, List<SubArea> subAreaList) {
        urn = dto.getUrn();
        name = dto.getName();
        internationalName = dto.getInternationalName();
        information = dto.getInformation();
        localizedInformation = dto.getLocalizedInformation();
        type = dto.getType();
        mandatoryLanguages = dto.getMandatoryLanguages();
        optionalLanguages = dto.getOptionalLanguages();
        description = dto.getDescription();
        this.maritimeService = maritimeService;
        subAreas = subAreaList;
        ranges = dto.getRanges().stream()
            .collect(toMap(range -> range[0], range -> range[1]));
        navtexSource = dto.getNavtexSource();
        s124Identifier = dto.getS124Identifier();
    }

    public SerieDetailsDto asDto() {
        return new SerieDetailsDto(this);
    }

    @PrePersist
    public void prePersist() {
        id = randomUUID();
    }
}
