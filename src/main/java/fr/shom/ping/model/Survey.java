package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.dto.SurveyExternalAnalysisResultsDto;
import fr.shom.ping.model.enums.SurveyActivity;
import fr.shom.ping.model.enums.SurveyBroadcastRestriction;
import fr.shom.ping.model.enums.SurveyCoordinatesOrder;
import fr.shom.ping.model.enums.SurveyFileComplianceAnalysisState;
import fr.shom.ping.model.enums.SurveyFileStatisticFields;
import fr.shom.ping.model.enums.SurveyIssuerType;
import fr.shom.ping.model.enums.SurveyState;
import fr.shom.ping.model.enums.SurveyTransmissionStatus;
import fr.shom.ping.model.enums.SurveyVerticalRef;
import fr.shom.ping.model.enums.SurveyZDirection;
import fr.shom.ping.model.tuple.SurveyTableBriTuple;
import fr.shom.ping.model.tuple.SurveyTableTuple;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.SqlResultSetMappings;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.locationtech.jts.geom.Geometry;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.GEOMETRY_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.GEOMETRY_EXAMPLE;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static fr.shom.ping.model.enums.SurveyState.DRAFT;
import static fr.shom.ping.utils.SchemaUtil.SURVEY_ADMIN_TABLE_BRI_VIEW_TUPLE_MAPPING;
import static fr.shom.ping.utils.SchemaUtil.SURVEY_ADMIN_TABLE_VIEW_TUPLE_MAPPING;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = NON_NULL)
@Schema(name = "Survey Model")
@SqlResultSetMappings(value = {
    @SqlResultSetMapping(name = SURVEY_ADMIN_TABLE_VIEW_TUPLE_MAPPING,
        classes = {
            @ConstructorResult(targetClass = SurveyTableTuple.class,
                columns = {
                    @ColumnResult(name = "id", type = UUID.class),
                    @ColumnResult(name = "identifier", type = String.class),
                    @ColumnResult(name = "name_and_location", type = String.class),
                    @ColumnResult(name = "last_update", type = Instant.class),
                    @ColumnResult(name = "start_date", type = Instant.class),
                    @ColumnResult(name = "end_date", type = Instant.class),
                    @ColumnResult(name = "compliant", type = String.class),
                    @ColumnResult(name = "test_processing_status", type = String.class),
                    @ColumnResult(name = "test_processing_date", type = Instant.class),
                    @ColumnResult(name = "transmission_status", type = String.class),
                    @ColumnResult(name = "last_update_transmission_date", type = Instant.class)
                })
        }
    ),
    @SqlResultSetMapping(name = SURVEY_ADMIN_TABLE_BRI_VIEW_TUPLE_MAPPING,
        classes = {
            @ConstructorResult(targetClass = SurveyTableBriTuple.class,
                columns = {
                    @ColumnResult(name = "id", type = UUID.class),
                    @ColumnResult(name = "identifier", type = String.class),
                    @ColumnResult(name = "shom_id", type = String.class),
                    @ColumnResult(name = "producer_organization", type = String.class),
                    @ColumnResult(name = "owner_organization", type = String.class),
                    @ColumnResult(name = "bri_internal_comment", type = String.class),
                    @ColumnResult(name = "name_and_location", type = String.class),
                    @ColumnResult(name = "last_update", type = Instant.class),
                    @ColumnResult(name = "start_date", type = Instant.class),
                    @ColumnResult(name = "end_date", type = Instant.class),
                    @ColumnResult(name = "compliant", type = String.class),
                    @ColumnResult(name = "test_processing_status", type = String.class),
                    @ColumnResult(name = "test_processing_date", type = Instant.class),
                    @ColumnResult(name = "transmission_status", type = String.class),
                    @ColumnResult(name = "last_update_transmission_date", type = Instant.class)
                })
        }
    )
})
public class Survey implements Serializable {

    @Serial
    private static final long serialVersionUID = -2049369282210875752L;

    @Id
    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @Column(unique = true)
    @NotEmpty(message = "validation.survey.identifier.not-empty")
    @NotBlank(message = "validation.survey.identifier.not-blank")
    @Schema(description = "PING identifier")
    private String identifier;

    @Schema(description = "SHOM identifier")
    private String shomId;

    @Column(unique = true)
    @NotEmpty(message = "validation.survey.name-and-location.not-empty")
    @NotBlank(message = "validation.survey.name-and-location.not-blank")
    @Schema(description = "Name and location")
    private String nameAndLocation;

    @ManyToOne(optional = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JsonIgnore
    @NotNull(message = "validation.survey.area.not-null")
    private Area area;

    @OneToOne
    @ToString.Exclude
    @Schema(description = "Issuing service")
    private MaritimeService issuingService;

    @OneToOne
    @ToString.Exclude
    @Schema(description = "Issuing service user")
    private ServiceUser issuingServiceUser;

    @OneToOne
    @ToString.Exclude
    @Schema(description = "Issuing public user")
    private PublicUser issuingPublicUser;

    @NotNull(message = "validation.survey.status.not-null")
    @Column(nullable = false)
    @Enumerated(STRING)
    @Schema(description = "Survey state")
    @Builder.Default
    private SurveyState state = DRAFT;

    @Schema(description = "Start date (UTC)")
    private Instant startDate;

    @Schema(description = "End date (UTC)")
    private Instant endDate;

    @Column(nullable = false)
    @Schema(description = "Last update date time (UTC)")
    private Instant lastUpdate;

    @Column(nullable = false)
    @Enumerated(STRING)
    @Schema(description = "Current transmission status")
    @Builder.Default
    private SurveyTransmissionStatus transmissionStatus = SurveyTransmissionStatus.NOT_AVAILABLE;

    @Schema(description = "last update transmission date (UTC)")
    private Instant lastUpdateTransmissionDate;

    @OneToMany(cascade = ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinTable(
        name = "survey_file_list",
        joinColumns = @JoinColumn(name = "survey_id"),
        inverseJoinColumns = @JoinColumn(name = "survey_file_id"))
    @ToString.Exclude
    @Schema(description = "Survey files (bathy or non-bathy)")
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private Set<SurveyFile> files = new HashSet<>();

    @OneToOne(cascade = ALL)
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @Schema(description = "Survey technical metadata file (PDF)")
    private Attachment technicalMetadataFile;

    @OneToMany(fetch = FetchType.EAGER, cascade = ALL, orphanRemoval = true)
    @JoinTable(
        name = "survey_attachment",
        joinColumns = @JoinColumn(name = "survey_id"),
        inverseJoinColumns = @JoinColumn(name = "attachment_id")
    )
    @ToString.Exclude
    @Schema(description = "Survey files")
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private Set<Attachment> miscAttachments = new HashSet<>();

    @Type(JsonType.class)
    @Column(columnDefinition = "json")
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    @Type(JsonType.class)
    @Column(columnDefinition = "json")
    @Builder.Default
    private Map<SurveyFileStatisticFields, String> statistics = new HashMap<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinTable(
        name = "survey_processing_result_list",
        joinColumns = @JoinColumn(name = "survey_id"),
        inverseJoinColumns = @JoinColumn(name = "processing_result_id"))
    @ToString.Exclude
    @Schema(description = "Processing results history")
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private Set<SurveyProcessingResult> processingResults = new HashSet<>();

    @ElementCollection(targetClass = SurveyIssuerType.class)
    @CollectionTable(
        name = "survey_issuer_type",
        joinColumns = @JoinColumn(name = "survey_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "issuer_type", nullable = false)
    private Set<SurveyIssuerType> surveyIssuerTypes;

    @Column(nullable = false)
    @NotNull(message = "validation.survey.geographic-format.not-null")
    @Builder.Default
    private boolean geographicFormat = true;

    @Schema(description = "Epsg code")
    private String epsg;

    @Enumerated(EnumType.STRING)
    private SurveyCoordinatesOrder coordinatesOrder;

    @Enumerated(EnumType.STRING)
    private SurveyZDirection zDirection;

    @Enumerated(EnumType.STRING)
    private SurveyVerticalRef verticalRef;

    private String fileSeparator;

    private Boolean externalAnalysisEnabled;

    private String goal;

    private String ownerOrganization;

    private String ownerEmail;

    private String producerOrganization;

    private String technicalManagerEmail;

    @Column(nullable = false)
    @Schema(description = "Creation date time (UTC)")
    private Instant creationDate;

    @Enumerated(EnumType.STRING)
    private SurveyActivity activityChoice;

    @Enumerated(EnumType.STRING)
    private SurveyBroadcastRestriction broadcastRestriction;

    private String confidentialityInformation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "validation.survey.compliant.not-null")
    @Builder.Default
    private SurveyFileComplianceAnalysisState compliant = SurveyFileComplianceAnalysisState.NOT_AVAILABLE;

    @OneToMany(mappedBy = "survey", fetch = LAZY, cascade = ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private Set<Notification> notifications = new HashSet<>();

    @Schema(description = GEOMETRY_DESCRIPTION, requiredMode = Schema.RequiredMode.REQUIRED, example = GEOMETRY_EXAMPLE)
    private Geometry boundingBox;

    @Column(name = "bri_internal_comment")
    private String briInternalComment;

    @Transient
    @EqualsAndHashCode.Exclude
    private SurveyExternalAnalysisResultsDto surveyExternalAnalysisResults;

    @Transient
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private boolean externalAnalysisInProgress = false;

    @Transient
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private boolean canBeReported = false;  // only if at least one analysis result received

    public UUID getUserId() {
        return issuingServiceUser != null ? issuingServiceUser.getId() : issuingPublicUser.getId();
    }

    @PrePersist
    public void prePersist() {
        id = randomUUID();
        creationDate = now();
        lastUpdate = now();
    }
}
