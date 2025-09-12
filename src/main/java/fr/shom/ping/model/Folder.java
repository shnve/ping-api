package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.enums.FolderStatus;
import fr.shom.ping.model.tuple.FolderTableTuple;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.SqlResultSetMappings;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
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
import static fr.shom.ping.model.enums.FolderStatus.OPEN;
import static fr.shom.ping.utils.SchemaUtil.FOLDER_TABLE_VIEW_TUPLE_MAPPING;
import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@JsonInclude(value = NON_NULL)
@Schema(name = "Folder Model")
@SqlResultSetMappings(value = {
    @SqlResultSetMapping(name = FOLDER_TABLE_VIEW_TUPLE_MAPPING,
        classes = {
            @ConstructorResult(targetClass = FolderTableTuple.class,
                columns = {
                    @ColumnResult(name = "id", type = UUID.class),
                    @ColumnResult(name = "number", type = Integer.class),
                    @ColumnResult(name = "issuingService", type = String.class),
                    @ColumnResult(name = "status", type = String.class),
                    @ColumnResult(name = "comment", type = String.class),
                    @ColumnResult(name = "object", type = String.class),
                    @ColumnResult(name = "creation_date", type = Instant.class),
                    @ColumnResult(name = "last_update", type = Instant.class),
                    @ColumnResult(name = "review_date", type = LocalDate.class),
                    @ColumnResult(name = "locality", type = String.class),
                    @ColumnResult(name = "area_name", type = String.class),
                    @ColumnResult(name = "area_international_name", type = String.class),
                    @ColumnResult(name = "areaId", type = UUID.class),
                    @ColumnResult(name = "issuing_service_id", type = UUID.class),
                    @ColumnResult(name = "entity_id", type = UUID.class),
                    @ColumnResult(name = "entity_name", type = String.class),
                    @ColumnResult(name = "entity_international_name", type = String.class),
                    @ColumnResult(name = "service_id", type = UUID.class),
                    @ColumnResult(name = "service_name", type = String.class),
                    @ColumnResult(name = "study_id", type = UUID.class),
                    @ColumnResult(name = "study_number", type = String.class),
                }
            )
        }
    )
})
public class Folder implements Serializable {
    @Serial
    private static final long serialVersionUID = -1019441437065621706L;

    @Id
    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @NotNull(message = "validation.folder.number.not-null")
    @Column(unique = true)
    @Schema(description = "Folder number")
    private Integer number;

    @NotNull(message = "validation.folder.status.not-null")
    @Column(nullable = false)
    @Enumerated(STRING)
    @Schema(description = "Folder status")
    @Default
    private FolderStatus status = OPEN;

    @Schema(description = "Internal comment")
    private String comment;

    @Schema(description = "Internal object")
    private String object;

    @Column(nullable = false)
    @Schema(description = "Creation date time (UTC)")
    private Instant creationDate;

    @Column(nullable = false)
    @Schema(description = "Last update date time (UTC)")
    private Instant lastUpdate;

    @Schema(description = "Review date time (UTC)")
    private LocalDate reviewDate;

    @OneToMany(mappedBy = "folder", orphanRemoval = true, cascade = ALL)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @Default
    private Set<Notion> notions = new HashSet<>();

    @OneToOne
    @ToString.Exclude
    @Schema(description = "Issuing service")
    private MaritimeService issuingService;

    @ManyToOne
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JsonIgnore
    private Area area;

    @ManyToOne
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JsonIgnore
    private SubArea locality;

    @ManyToMany
    @JoinTable(
        name = "folder_maritime_entity",
        joinColumns = @JoinColumn(name = "folder_id"),
        inverseJoinColumns = @JoinColumn(name = "maritime_entity_id"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Default
    private Set<MaritimeEntity> entities = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "folder_maritime_service",
        joinColumns = @JoinColumn(name = "folder_id"),
        inverseJoinColumns = @JoinColumn(name = "maritime_service_id"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Default
    private Set<MaritimeService> services = new HashSet<>();

    @OneToMany(mappedBy = "folder", fetch = LAZY, orphanRemoval = true, cascade = ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Default
    private Set<Study> studies = new HashSet<>();

    @OneToMany(mappedBy = "folder", cascade = ALL)
    @Schema(description = "List of folder attachment")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @Default
    private Set<FolderAttachment> attachments = new HashSet<>();

    @OneToMany(mappedBy = "folder", fetch = LAZY, cascade = ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Default
    private Set<Notification> notifications = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "folder_following",
        joinColumns = @JoinColumn(name = "folder_id"),
        inverseJoinColumns = @JoinColumn(name = "maritime_service_id"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Default
    private Set<MaritimeService> followerServices = new HashSet<>();

    @PrePersist
    public void prePersist() {
        id = randomUUID();
        creationDate = now();
        lastUpdate = now();
    }
}
