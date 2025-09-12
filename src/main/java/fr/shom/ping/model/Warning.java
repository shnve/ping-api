package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.dto.WarningCreateDto;
import fr.shom.ping.model.enums.WarningHazardTypeGeneral;
import fr.shom.ping.model.enums.WarningStatus;
import fr.shom.ping.model.tuple.WarningAdminTableTuple;
import fr.shom.ping.model.tuple.WarningDetailedTableViewTuple;
import fr.shom.ping.model.tuple.WarningMapViewTuple;
import fr.shom.ping.model.tuple.WarningTableViewTuple;
import fr.shom.ping.validation.ValidCountryCode;
import fr.shom.ping.validation.ValidURN;
import io.hypersistence.utils.hibernate.type.json.JsonNodeStringType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.SqlResultSetMappings;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.locationtech.jts.geom.Geometry;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newTreeSet;
import static fr.shom.ping.SwaggerConfig.Property.COUNTRY_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static fr.shom.ping.utils.SchemaUtil.LOCALIZED_TEXT_TABLE;
import static fr.shom.ping.utils.SchemaUtil.WARNING_ADMIN_TABLE_VIEW_TUPLE_MAPPING;
import static fr.shom.ping.utils.SchemaUtil.WARNING_DETAILED_TABLE_VIEW_TUPLE_MAPPING;
import static fr.shom.ping.utils.SchemaUtil.WARNING_MAP_VIEW_TUPLE_MAPPING;
import static fr.shom.ping.utils.SchemaUtil.WARNING_TABLE_VIEW_TUPLE_MAPPING;
import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.CascadeType.REMOVE;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.EAGER;
import static jakarta.persistence.FetchType.LAZY;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.springframework.util.CollectionUtils.isEmpty;

@Entity
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = NON_NULL)
@Schema(name = "Warning Model")
@SqlResultSetMappings(value = {
    @SqlResultSetMapping(name = WARNING_TABLE_VIEW_TUPLE_MAPPING,
        classes = {
            @ConstructorResult(targetClass = WarningTableViewTuple.class,
                columns = {
                    @ColumnResult(name = "id", type = UUID.class),
                    @ColumnResult(name = "name_of_series", type = String.class),
                    @ColumnResult(name = "warning_number", type = Integer.class),
                    @ColumnResult(name = "year", type = Integer.class),
                    @ColumnResult(name = "general_area_id", type = UUID.class),
                    @ColumnResult(name = "sub_area_id", type = UUID.class),
                    @ColumnResult(name = "general_area_position", type = Integer.class),
                    @ColumnResult(name = "general_area_name", type = String.class),
                    @ColumnResult(name = "general_area_international_name", type = String.class),
                    @ColumnResult(name = "locality_id", type = UUID.class),
                    @ColumnResult(name = "locality_parent_id", type = UUID.class),
                    @ColumnResult(name = "locality_position", type = Integer.class),
                    @ColumnResult(name = "locality_name", type = String.class),
                    @ColumnResult(name = "locality_international_name", type = String.class),
                    @ColumnResult(name = "warning_hazard_type_general", type = String.class),
                    @ColumnResult(name = "warning_hazard_type_detail", type = String.class)
                })
        }
    ),
    @SqlResultSetMapping(name = WARNING_MAP_VIEW_TUPLE_MAPPING,
        classes = {
            @ConstructorResult(targetClass = WarningMapViewTuple.class,
                columns = {
                    @ColumnResult(name = "id", type = UUID.class),
                    @ColumnResult(name = "name_of_series", type = String.class),
                    @ColumnResult(name = "warning_number", type = Integer.class),
                    @ColumnResult(name = "year", type = Integer.class),
                    @ColumnResult(name = "warning_hazard_type_general", type = String.class),
                    @ColumnResult(name = "warning_hazard_type_details", type = String.class),
                    @ColumnResult(name = "serie_type", type = String.class),
                    @ColumnResult(name = "publication_time", type = Instant.class),
                    @ColumnResult(name = "geometry_properties", type = JsonNodeStringType.class),
                    @ColumnResult(name = "geometry", type = Geometry.class),
                    @ColumnResult(name = "affected_area_properties", type = JsonNodeStringType.class),
                    @ColumnResult(name = "affected_area", type = Geometry.class)
                })
        }
    ),
    @SqlResultSetMapping(name = WARNING_DETAILED_TABLE_VIEW_TUPLE_MAPPING,
        classes = {
            @ConstructorResult(targetClass = WarningDetailedTableViewTuple.class,
                columns = {
                    @ColumnResult(name = "id", type = UUID.class),
                    @ColumnResult(name = "name_of_series", type = String.class),
                    @ColumnResult(name = "warning_number", type = Integer.class),
                    @ColumnResult(name = "year", type = Integer.class),
                    @ColumnResult(name = "general_area_id", type = UUID.class),
                    @ColumnResult(name = "sub_area_id", type = UUID.class),
                    @ColumnResult(name = "general_area_position", type = Integer.class),
                    @ColumnResult(name = "general_area_name", type = String.class),
                    @ColumnResult(name = "general_area_international_name", type = String.class),
                    @ColumnResult(name = "locality_id", type = UUID.class),
                    @ColumnResult(name = "locality_parent_id", type = UUID.class),
                    @ColumnResult(name = "locality_position", type = Integer.class),
                    @ColumnResult(name = "locality_name", type = String.class),
                    @ColumnResult(name = "locality_international_name", type = String.class),
                    @ColumnResult(name = "warning_hazard_type_general", type = String.class),
                    @ColumnResult(name = "feature_position", type = Integer.class),
                    @ColumnResult(name = "warning_hazard_type_detail", type = String.class),
                    @ColumnResult(name = "feature_information_language", type = String.class),
                    @ColumnResult(name = "feature_information_text", type = String.class),
                    @ColumnResult(name = "date_position", type = Integer.class),
                    @ColumnResult(name = "date_end", type = LocalDate.class),
                    @ColumnResult(name = "date_start", type = LocalDate.class),
                    @ColumnResult(name = "time_of_day_end", type = LocalTime.class),
                    @ColumnResult(name = "time_of_day_start", type = LocalTime.class),
                    @ColumnResult(name = "period_timezone", type = String.class),
                    @ColumnResult(name = "cancellation_date", type = Instant.class),
                    @ColumnResult(name = "creation_date", type = Instant.class),
                    @ColumnResult(name = "publication_time", type = Instant.class),
                    @ColumnResult(name = "reference_name_of_series", type = String.class),
                    @ColumnResult(name = "reference_warning_number", type = Integer.class),
                    @ColumnResult(name = "reference_year", type = Integer.class),
                })
        }
    ),
    @SqlResultSetMapping(name = WARNING_ADMIN_TABLE_VIEW_TUPLE_MAPPING,
        classes = {
            @ConstructorResult(targetClass = WarningAdminTableTuple.class,
                columns = {
                    @ColumnResult(name = "id", type = UUID.class),
                    @ColumnResult(name = "name_of_series", type = String.class),
                    @ColumnResult(name = "warning_number", type = Integer.class),
                    @ColumnResult(name = "year", type = Integer.class),
                    @ColumnResult(name = "model_name", type = String.class),
                    @ColumnResult(name = "general_area_id", type = UUID.class),
                    @ColumnResult(name = "sub_area_id", type = UUID.class),
                    @ColumnResult(name = "general_area_position", type = Integer.class),
                    @ColumnResult(name = "general_area_name", type = String.class),
                    @ColumnResult(name = "general_area_international_name", type = String.class),
                    @ColumnResult(name = "locality_id", type = UUID.class),
                    @ColumnResult(name = "locality_parent_id", type = UUID.class),
                    @ColumnResult(name = "locality_position", type = Integer.class),
                    @ColumnResult(name = "locality_name", type = String.class),
                    @ColumnResult(name = "locality_international_name", type = String.class),
                    @ColumnResult(name = "warning_hazard_type_general", type = String.class),
                    @ColumnResult(name = "warning_hazard_type_detail", type = String.class),
                    @ColumnResult(name = "creation_date", type = Instant.class),
                    @ColumnResult(name = "last_update", type = Instant.class),
                    @ColumnResult(name = "estimated_publication_date", type = LocalDate.class),
                    @ColumnResult(name = "publication_time", type = Instant.class),
                    @ColumnResult(name = "cancellation_date", type = Instant.class),
                    @ColumnResult(name = "end_of_broadcast_date", type = LocalDate.class),
                    @ColumnResult(name = "review_date", type = LocalDate.class),
                    @ColumnResult(name = "internal_comment", type = String.class),
                    @ColumnResult(name = "warning_state_order", type = Integer.class),
                    @ColumnResult(name = "reference_name_of_series", type = String.class),
                    @ColumnResult(name = "reference_warning_number", type = Integer.class),
                    @ColumnResult(name = "reference_year", type = Integer.class),
                    @ColumnResult(name = "status", type = String.class),
                    @ColumnResult(name = "entity", type = String.class),
                    @ColumnResult(name = "folder", type = String.class)
                })
        }
    )
})
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"serie_id", "warningNumber", "year"}))
public class Warning implements Serializable {
    @Serial
    private static final long serialVersionUID = 6266341298395425466L;

    @Id
    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @NotNull(message = "validation.warning.serie.not-null")
    @OneToOne(optional = false)
    @Schema(description = "Serie", required = true)
    private Serie serie;

    @ValidCountryCode(message = "validation.warning.country-code.invalid")
    @Schema(description = COUNTRY_DESCRIPTION)
    private String country;

    @NotEmpty(message = "validation.warning.name-of-series.not-empty")
    @Column(nullable = false)
    @Schema(description = "Name of series", required = true)
    private String nameOfSeries;

    @NotEmpty(message = "validation.warning.production-agency.not-empty")
    @Column(nullable = false)
    @Schema(description = "Production agency", required = true)
    private String productionAgency;

    @ValidURN(message = "validation.warning.warning-identifier.invalid")
    @Schema(description = "Warning identifier. Should be a Uniform resource name (RFC 8141 compliant)")
    private String warningIdentifier;

    @Schema(description = "Warning identifier")
    private Integer warningNumber;

    @Schema(description = "Year")
    private Integer year;

    @NotNull(message = "validation.warning.status.not-null")
    @Column(nullable = false)
    @Enumerated(STRING)
    private WarningStatus status;

    @JsonIgnore
    @ElementCollection
    @CollectionTable(
        name = LOCALIZED_TEXT_TABLE,
        joinColumns = @JoinColumn(name = "parent_id")
    )
    @EqualsAndHashCode.Exclude
    @Default
    @ToString.Exclude
    private Set<LocalizedText> titles = newHashSet();

    @OneToMany(mappedBy = "warning", fetch = EAGER, cascade = ALL, orphanRemoval = true)
    @Schema(description = "List of general area. Sorted.")
    @EqualsAndHashCode.Exclude
    @Default
    private SortedSet<GeneralArea> generalAreas = newTreeSet();

    @OneToMany(mappedBy = "warning", fetch = EAGER, cascade = ALL, orphanRemoval = true)
    @Schema(description = "List of locality. Sorted.")
    @EqualsAndHashCode.Exclude
    @Default
    private List<WarningLocalityNames> localities = newArrayList();

    @NotNull(message = "validation.warning.warning-hazard-type-general.not-null")
    @Column(nullable = false)
    @Enumerated(STRING)
    @Schema(description = "Warning hazard type general", required = true)
    private WarningHazardTypeGeneral warningHazardTypeGeneral;

    @Column(nullable = false)
    private Instant creationDate;

    @Schema(description = "Publication time")
    private Instant publicationTime;

    @Schema(description = "Cancellation date")
    private Instant cancellationDate;

    @Schema(description = "Show cancellation date")
    @Default
    private boolean addCancellationTimeToTelex = true;

    @OneToMany(mappedBy = "warning", fetch = EAGER, cascade = ALL, orphanRemoval = true)
    @EqualsAndHashCode.Exclude
    @Schema(description = "List of feature part")
    @Default
    private SortedSet<FeaturePart> featureParts = newTreeSet();

    private LocalDate estimatedPublicationDate;

    private LocalDate reviewDate;

    private LocalDate endOfBroadcastDate;

    private Instant lastUpdate;

    @OneToOne
    private ProUser lastUpdateUser;

    private String internalComment;

    @OneToMany
    @JoinColumn(name = "warning_id", nullable = false)
    @Default
    @EqualsAndHashCode.Exclude
    private Set<WarningPublicComment> publicComments = newHashSet();

    @OneToMany(mappedBy = "warning", cascade = ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Default
    private Set<WarningReference> warningReferences = new HashSet<>();

    @OneToMany(mappedBy = "warning", cascade = ALL, orphanRemoval = true)
    @EqualsAndHashCode.Exclude
    @Default
    private Set<WarningAttachment> attachments = new HashSet<>();

    private String telex;

    private String navtex;

    @OneToOne(fetch = LAZY, cascade = ALL, mappedBy = "warning", orphanRemoval = true)
    @Schema(description = "NAVTEX configuration")
    @ToString.Exclude
    private WarningNavtexConfig navtexConfig;

    @OneToMany
    @JoinTable(
        name = "warning_infosource",
        joinColumns = @JoinColumn(name = "warning_id"),
        inverseJoinColumns = @JoinColumn(name = "infosource_id"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Default
    private Set<Infosource> infosources = new HashSet<>();

    @OneToMany(mappedBy = "warning", fetch = LAZY, orphanRemoval = true, cascade = ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Default
    private Set<WarningGeoregulation> georegulations = new HashSet<>();

    @OneToMany
    @JoinTable(
        name = "warning_entity",
        joinColumns = @JoinColumn(name = "warning_id"),
        inverseJoinColumns = @JoinColumn(name = "entity_id"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Default
    private Set<MaritimeEntity> maritimeEntities = new HashSet<>();

    @OneToMany(fetch = LAZY, cascade = REMOVE, mappedBy = "warning")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Default
    private Set<NotionWarning> notions = new HashSet<>();

    @OneToMany(mappedBy = "warning", fetch = LAZY, cascade = ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Default
    private Set<Notification> notifications = new HashSet<>();

    @Default
    private Boolean forceToReview = false;

    @Schema(description = "Model name")
    private String modelName;

    @OneToMany(mappedBy = "warning", fetch = EAGER)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private Set<WarningLink> warningLinksPublicsReferences = new HashSet<>();

    @OneToMany
    @JoinTable(
        name = "warning_maritime_entity_reference",
        joinColumns = @JoinColumn(name = "warning_id"),
        inverseJoinColumns = @JoinColumn(name = "maritime_entity_reference_id"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private Set<MaritimeEntityReference> entityReferences = new HashSet<>();

    @OneToMany(mappedBy = "warning", fetch = LAZY, orphanRemoval = true, cascade = ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private Set<WarningPublicReference> warningsPublicsReferences = new HashSet<>();

    @OneToOne(fetch = LAZY)
    @JoinTable(
        name = "warning_survey",
        joinColumns = @JoinColumn(name = "warning_id"),
        inverseJoinColumns = @JoinColumn(name = "survey_id"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Survey survey;

    @Default
    @Column(nullable = false)
    private Boolean isBathy = false;

    @Schema(description = "Serie S124 Identifier")
    @Column(name = "serie_s124_identifier")
    private String serieS124Identifier;

    @Schema(description = "Maritime Service OHI producer code")
    @Column(name = "maritime_service_iho_producer_code")
    private String maritimeServiceihoProducerCode;

    public Warning(WarningCreateDto dto, Serie serie, SortedSet<GeneralArea> generalAreaList, List<WarningLocalityNames> localityList,
        Set<WarningAttachment> attachmentList, Set<WarningReference> warningReferenceList, SortedSet<FeaturePart> featurePartList) {
        this.serie = serie;
        this.generalAreas = generalAreaList;
        this.localities = localityList;
        this.warningHazardTypeGeneral = dto.getTypeGeneral();
        this.warningNumber = dto.getWarningNumber();
        this.year = dto.getYear();
        this.warningReferences = warningReferenceList;
        this.featureParts = featurePartList;
        this.attachments = attachmentList;
        this.estimatedPublicationDate = dto.getEstimatedPublicationDate();
        this.reviewDate = dto.getReviewDate();
        this.cancellationDate = dto.getCancellationTime();
        this.addCancellationTimeToTelex = dto.isAddCancellationTimeToTelex();
        this.internalComment = dto.getInternalComment();
        this.status = dto.getStatus();
        this.telex = dto.getTelex();
        this.navtex = dto.getNavtex();
        this.navtexConfig = dto.getNavtexConfig();
        this.serieS124Identifier = serie.getS124Identifier();
        this.maritimeServiceihoProducerCode = serie.getMaritimeService().getIhoProducerCode();
    }

    @PrePersist
    public void prePersist() {
        id = randomUUID();
        creationDate = now();
        this.setWarningToChildren();
    }

    @PreUpdate
    public void preUpdate() {
        this.setWarningToLinkAndPublicsReferences();
    }

    public void setWarningToLinkAndPublicsReferences() {
        if (!isEmpty(attachments)) {
            attachments.forEach(warningAttachment -> warningAttachment.setWarning(this));
        }
        if (!isEmpty(georegulations)) {
            georegulations.forEach(warningGeoregulation -> warningGeoregulation.setWarning(this));
        }
        if (!isEmpty(warningsPublicsReferences)) {
            warningsPublicsReferences.forEach(warningGeoregulation -> warningGeoregulation.setWarning(this));
        }
    }

    private void setWarningToChildren() {
        if (!isEmpty(featureParts)) {
            featureParts.forEach(featurePart -> featurePart.setWarning(this));
        }
        if (!isEmpty(generalAreas)) {
            generalAreas.forEach(generalArea -> generalArea.setWarning(this));
        }
        if (!isEmpty(localities)) {
            localities.forEach(locality -> locality.setWarning(this));
        }
        if (!isEmpty(warningReferences)) {
            warningReferences.forEach(warningReference -> warningReference.setWarning(this));
        }

        this.setWarningToLinkAndPublicsReferences();

        // if warning config is not null, then link warning and navtex config
        if (navtexConfig != null) {
            navtexConfig.setWarning(this);
        }
    }

    public static String formatWarningFullNumber(Warning warning) {
        String nameOfSeries = warning.getNameOfSeries();
        String warningNumber = warning.getWarningNumber().toString();
        String warningYear = Year.of(warning.getYear()).format(DateTimeFormatter.ofPattern("uu"));
        return nameOfSeries + " " + warningNumber + "/" + warningYear;
    }

    public String formattedWarningFullNumber() {
        return formatWarningFullNumber(this);
    }
}
