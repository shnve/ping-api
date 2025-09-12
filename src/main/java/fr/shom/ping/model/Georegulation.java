package fr.shom.ping.model;

import fr.shom.ping.model.enums.GeoregulationActivity;
import fr.shom.ping.model.enums.GeoregulationStatus;
import fr.shom.ping.model.enums.GeoregulationType;
import fr.shom.ping.model.tuple.GeoregulationAdminTableTuple;
import fr.shom.ping.model.tuple.GeoregulationMapViewTuple;
import io.hypersistence.utils.hibernate.type.json.JsonNodeStringType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.SqlResultSetMappings;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Geometry;

import static fr.shom.ping.model.enums.GeoregulationStatus.DRAFT;
import static fr.shom.ping.utils.SchemaUtil.GEOREGULATION_ADMIN_TABLE_TUPLE_MAPPING;
import static fr.shom.ping.utils.SchemaUtil.GEOREGULATION_MAP_VIEW_TUPLE_MAPPING;
import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.CascadeType.REMOVE;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
@SqlResultSetMappings(value = {
    @SqlResultSetMapping(name = GEOREGULATION_ADMIN_TABLE_TUPLE_MAPPING,
        classes = {
            @ConstructorResult(targetClass = GeoregulationAdminTableTuple.class,
                columns = {
                    @ColumnResult(name = "id", type = UUID.class),
                    @ColumnResult(name = "status", type = String.class),
                    @ColumnResult(name = "main_number_year", type = Integer.class),
                    @ColumnResult(name = "main_number_text", type = String.class),
                    @ColumnResult(name = "main_number_authority_code", type = String.class),
                    @ColumnResult(name = "main_number_date", type = LocalDate.class),
                    @ColumnResult(name = "type", type = String.class),
                    @ColumnResult(name = "activity", type = String.class),
                    @ColumnResult(name = "title", type = String.class),
                    @ColumnResult(name = "international_title", type = String.class),
                    @ColumnResult(name = "maritime_entity_id", type = UUID.class),
                    @ColumnResult(name = "maritime_entity_name", type = String.class),
                    @ColumnResult(name = "maritime_entity_international_name", type = String.class),
                    @ColumnResult(name = "internal_comment", type = String.class),
                    @ColumnResult(name = "last_update", type = Instant.class),
                    @ColumnResult(name = "publication_date", type = Instant.class),
                    @ColumnResult(name = "repeal_date", type = Instant.class),
                    @ColumnResult(name = "is_public", type = Boolean.class),
                    @ColumnResult(name = "is_new", type = Boolean.class),
                    @ColumnResult(name = "is_expired", type = Boolean.class)
                })
        }
    ),
    @SqlResultSetMapping(name = GEOREGULATION_MAP_VIEW_TUPLE_MAPPING,
        classes = {
            @ConstructorResult(targetClass = GeoregulationMapViewTuple.class,
                columns = {
                    @ColumnResult(name = "id", type = UUID.class),
                    @ColumnResult(name = "number_year", type = Integer.class),
                    @ColumnResult(name = "number_text", type = String.class),
                    @ColumnResult(name = "number_authority_code", type = String.class),
                    @ColumnResult(name = "number_date", type = LocalDate.class),
                    @ColumnResult(name = "is_temporary", type = Boolean.class),
                    @ColumnResult(name = "is_new", type = Boolean.class),
                    @ColumnResult(name = "publication_date", type = LocalDate.class),
                    @ColumnResult(name = "geometry_properties", type = JsonNodeStringType.class),
                    @ColumnResult(name = "geometry", type = Geometry.class)
                })
        }
    ),
})
public class Georegulation implements Serializable {
    @Serial
    private static final long serialVersionUID = -193722362059079714L;

    @Id
    private UUID id;

    @Enumerated(STRING)
    @Column(nullable = false)
    @Default
    private GeoregulationStatus status = DRAFT;

    @ManyToOne(optional = false, fetch = LAZY)
    @ToString.Exclude
    private MaritimeService issuingService;

    @Enumerated(STRING)
    @Column(nullable = false)
    private GeoregulationType type;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "year", column = @Column(name = "number_year")),
        @AttributeOverride(name = "text", column = @Column(name = "number_text")),
        @AttributeOverride(name = "date", column = @Column(name = "number_date"))
    })
    private GeoregulationNumber number;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    @Default
    private List<GeoregulationNumber> additionalNumbers = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
        name = "georegulation_activity"
    )
    @Enumerated(STRING)
    @Column(nullable = false, name = "activity")
    @Default
    private List<GeoregulationActivity> activities = new ArrayList<>();

    private String title;

    private String internationalTitle;

    @OneToMany(fetch = LAZY)
    @JoinTable(
        name = "georegulation_entity",
        inverseJoinColumns = @JoinColumn(name = "entity_id")
    )
    @ToString.Exclude
    @Default
    private List<MaritimeEntity> entities = new ArrayList<>();

    private String summary;

    private String internationalSummary;

    private boolean isTemporary;

    @ElementCollection
    @CollectionTable(
        name = "fixed_date_range",
        joinColumns = @JoinColumn(name = "parent_id")
    )
    @Default
    private SortedSet<FixedDateRange> calendarPeriods = new TreeSet<>();

    @ElementCollection
    @CollectionTable(
        name = "georegulation_seasonal_period",
        joinColumns = @JoinColumn(name = "georegulation_id")
    )
    @Default
    private SortedSet<SeasonalPeriod> seasonalPeriods = new TreeSet<>();

    private boolean hasCoastlineLimit;

    @OneToMany(orphanRemoval = true, cascade = ALL)
    @JoinColumn(name = "georegulation_id", nullable = false)
    @Default
    private SortedSet<GeoregulationPart> parts = new TreeSet<>();

    @ElementCollection
    @CollectionTable(
        name = "georegulation_link",
        joinColumns = @JoinColumn(name = "georegulation_id")
    )
    @Column(name = "link")
    @Default
    private List<String> links = new ArrayList<>();

    @OneToMany(cascade = ALL, orphanRemoval = true)
    @JoinTable(
        name = "georegulation_attachment",
        joinColumns = @JoinColumn(name = "georegulation_id"),
        inverseJoinColumns = @JoinColumn(name = "attachment_id")
    )
    @Default
    private List<Attachment> attachments = new ArrayList<>();

    private boolean isPublic;

    private Integer markAsNewInMonths;

    private String internalComment;

    @Column(nullable = false)
    private String timezone;

    @Column(nullable = false)
    private Instant creationDate;

    @Column(nullable = false)
    private Instant lastUpdate;

    @ManyToOne(optional = false)
    private ServiceUser lastUpdateUser;

    private Instant publicationDate;

    private Instant repealDate;

    private boolean isExpired;

    private boolean isNew;

    @OneToOne(fetch = LAZY, mappedBy = "georegulation")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Infosource infosource;

    @OneToMany(fetch = LAZY)
    @JoinTable(
        name = "warning_georegulation",
        joinColumns = @JoinColumn(name = "georegulation_id"),
        inverseJoinColumns = @JoinColumn(name = "warning_id"))
    @Default
    private Set<Warning> warnings = new HashSet<>();

    @OneToMany(fetch = LAZY, cascade = REMOVE, mappedBy = "georegulation")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Default
    private Set<NotionGeoregulation> notions = new HashSet<>();

    @OneToMany(mappedBy = "georegulation", fetch = LAZY, cascade = ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Default
    private Set<Notification> notifications = new HashSet<>();

    @Default
    @Column(nullable = false)
    private boolean isAllTerritorialSea = false;

    @Default
    @Column(nullable = false)
    private boolean isAnnouncedByAvurnavAvirade = false;

    @PrePersist
    public void prePersist() {
        id = randomUUID();
        Instant now = now();
        creationDate = now;
        lastUpdate = now;
    }
}
