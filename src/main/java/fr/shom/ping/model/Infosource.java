package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.enums.InfosourceNature;
import fr.shom.ping.model.enums.InfosourceStatus;
import fr.shom.ping.model.tuple.InfosourceAdminTableTuple;
import fr.shom.ping.model.tuple.InfosourceTableTuple;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
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
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
import org.hibernate.annotations.Type;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.google.common.collect.Sets.newTreeSet;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static fr.shom.ping.model.enums.InfosourceStatus.DRAFT;
import static fr.shom.ping.utils.SchemaUtil.INFOSOURCE_ADMIN_TABLE_VIEW_TUPLE_MAPPING;
import static fr.shom.ping.utils.SchemaUtil.INFOSOURCE_TABLE_VIEW_TUPLE_MAPPING;
import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.CascadeType.REMOVE;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.EAGER;
import static jakarta.persistence.FetchType.LAZY;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = NON_NULL)
@Schema(name = "Infosource Model")
@SqlResultSetMappings(value = {
    @SqlResultSetMapping(name = INFOSOURCE_ADMIN_TABLE_VIEW_TUPLE_MAPPING,
        classes = {
            @ConstructorResult(targetClass = InfosourceAdminTableTuple.class,
                columns = {
                    @ColumnResult(name = "id", type = UUID.class),
                    @ColumnResult(name = "number", type = String.class),
                    @ColumnResult(name = "nature", type = String.class),
                    @ColumnResult(name = "creation_date", type = Instant.class),
                    @ColumnResult(name = "last_update", type = Instant.class),
                    @ColumnResult(name = "validation_time", type = Instant.class),
                    @ColumnResult(name = "comment", type = String.class),
                    @ColumnResult(name = "status", type = String.class),
                    @ColumnResult(name = "issuing_service_user_id", type = UUID.class),
                    @ColumnResult(name = "issuing_service_id", type = UUID.class),
                    @ColumnResult(name = "description", type = String.class),
                    @ColumnResult(name = "action_type", type = String.class),
                    @ColumnResult(name = "recipient_id", type = UUID.class),
                    @ColumnResult(name = "recipient_name", type = String.class),
                    @ColumnResult(name = "recipient_information", type = String.class),
                    @ColumnResult(name = "recipient_localized_information", type = String.class),
                    @ColumnResult(name = "transferred_by", type = String.class),
                    @ColumnResult(name = "emitter", type = String.class),
                    @ColumnResult(name = "service_status", type = String.class),
                    @ColumnResult(name = "entity_id", type = UUID.class),
                    @ColumnResult(name = "entity_name", type = String.class),
                    @ColumnResult(name = "service_tag", type = String.class),
                    @ColumnResult(name = "folder_number", type = String.class),
                    @ColumnResult(name = "warning_number", type = String.class),
                    @ColumnResult(name = "reference_study_id", type = UUID.class),
                    @ColumnResult(name = "reference_type", type = String.class),
                    @ColumnResult(name = "reference_value", type = String.class),
                })
        }
    ),
    @SqlResultSetMapping(name = INFOSOURCE_TABLE_VIEW_TUPLE_MAPPING,
        classes = {
            @ConstructorResult(targetClass = InfosourceTableTuple.class,
                columns = {
                    @ColumnResult(name = "id", type = UUID.class),
                    @ColumnResult(name = "number", type = String.class),
                    @ColumnResult(name = "nature", type = String.class),
                    @ColumnResult(name = "creation_date", type = Instant.class),
                    @ColumnResult(name = "last_update", type = Instant.class),
                    @ColumnResult(name = "validation_time", type = Instant.class),
                    @ColumnResult(name = "comment", type = String.class),
                    @ColumnResult(name = "status", type = String.class),
                    @ColumnResult(name = "issuing_public_user_id", type = UUID.class),
                    @ColumnResult(name = "description", type = String.class),
                    @ColumnResult(name = "recipient_id", type = UUID.class),
                    @ColumnResult(name = "recipient_name", type = String.class),
                    @ColumnResult(name = "recipient_information", type = String.class),
                    @ColumnResult(name = "recipient_localized_information", type = String.class),
                    @ColumnResult(name = "action_type", type = String.class),
                    @ColumnResult(name = "warning_number", type = String.class),
                    @ColumnResult(name = "reference_study_id", type = UUID.class),
                    @ColumnResult(name = "reference_type", type = String.class),
                    @ColumnResult(name = "reference_value", type = String.class)
                })
        }
    )
})
public class Infosource implements Serializable {
    @Serial
    private static final long serialVersionUID = -5047979737033764737L;
    @Id
    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @Column(unique = true)
    @Schema(description = "infosource number")
    private String number;

    @NotEmpty(message = "validation.infosource.infosource-parts.not-empty")
    @OneToMany(mappedBy = "infosource", fetch = EAGER, cascade = ALL, orphanRemoval = true)
    @EqualsAndHashCode.Exclude
    @Schema(description = "List of infosource parts")
    @Default
    private SortedSet<InfosourcePart> infosourceParts = newTreeSet();

    @NotNull(message = "validation.infosource.nature.not-null")
    @Column(nullable = false)
    @Enumerated(STRING)
    @Schema(description = "Infosource nature")
    private InfosourceNature nature;

    @NotNull(message = "validation.infosource.status.not-null")
    @Column(nullable = false)
    @Enumerated(STRING)
    @Schema(description = "Infosource status")
    @Default
    private InfosourceStatus status = DRAFT;

    @OneToOne
    @ToString.Exclude
    @Schema(description = "Issuing service")
    private MaritimeService issuingService;

    @OneToOne
    @ToString.Exclude
    @Schema(description = "Issuing public user")
    private PublicUser issuingPublicUser;

    @OneToOne
    @ToString.Exclude
    private ServiceUser issuingServiceUser;

    @Schema(description = "Internal comment")
    private String comment;

    @Column(nullable = false)
    @Schema(description = "Creation date time (UTC)")
    private Instant creationDate;

    @Column(nullable = false)
    @Schema(description = "Last update date time (UTC)")
    private Instant lastUpdate;

    @Schema(description = "Sending date time (UTC)")
    private Instant validationTime;

    @OneToMany(cascade = ALL, orphanRemoval = true)
    @JoinTable(
        name = "infosource_attachment",
        joinColumns = @JoinColumn(name = "infosource_id"),
        inverseJoinColumns = @JoinColumn(name = "attachment_id"))
    @EqualsAndHashCode.Exclude
    @Default
    private Set<Attachment> attachments = new HashSet<>();

    @Schema(description = "Timezone")
    private String timezone;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Default
    @OneToMany(mappedBy = "infosource", fetch = EAGER, cascade = ALL)
    private Set<InfosourceRecipient> recipients = new HashSet<>();

    @Type(JsonType.class)
    @Column(columnDefinition = "json")
    @Default
    private Map<String, Object> object = new HashMap<>();

    @OneToMany
    @JoinTable(
        name = "infosource_maritime_entity",
        joinColumns = @JoinColumn(name = "infosource_id"),
        inverseJoinColumns = @JoinColumn(name = "maritime_entity_id"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Default
    private Set<MaritimeEntity> entities = new HashSet<>();

    @OneToMany
    @JoinTable(
        name = "infosource_tag",
        joinColumns = @JoinColumn(name = "infosource_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Default
    private Set<Tag> tags = new HashSet<>();

    @OneToMany
    @JoinTable(
        name = "warning_infosource",
        joinColumns = @JoinColumn(name = "infosource_id"),
        inverseJoinColumns = @JoinColumn(name = "warning_id"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Default
    private Set<Warning> linkedWarnings = new HashSet<>();

    @OneToOne(fetch = LAZY)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Georegulation georegulation;

    @OneToOne(fetch = LAZY)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Infosource parentInfosource;

    @OneToMany(fetch = LAZY, cascade = REMOVE, mappedBy = "infosource")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Default
    private Set<NotionInfosource> notions = new HashSet<>();

    @Schema(description = "Start date (UTC)")
    private Instant startDate;

    @Schema(description = "End date (UTC)")
    private Instant endDate;

    private String confidentialityInformation;

    @ManyToOne(fetch = LAZY)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Survey survey;

    @PrePersist
    public void prePersist() {
        id = randomUUID();
        creationDate = now();
        lastUpdate = now();
        if (isNotEmpty(infosourceParts)) {
            infosourceParts.forEach(infosourcePart -> infosourcePart.setInfosource(this));
        }
    }
}
