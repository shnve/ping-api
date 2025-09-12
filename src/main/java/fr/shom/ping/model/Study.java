package fr.shom.ping.model;

import fr.shom.ping.model.dto.StudyCreateDto;
import fr.shom.ping.model.dto.StudyDto;
import fr.shom.ping.model.enums.StudyStatus;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static fr.shom.ping.utils.SchemaUtil.STUDY_REFERENCE_TABLE;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;

@Data
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Study implements Serializable {
    @Serial
    private static final long serialVersionUID = -5332278225137911109L;

    @Id
    private UUID id;

    @OneToOne(optional = false, fetch = LAZY)
    @ToString.Exclude
    private Folder folder;

    @NotNull(message = "validation.study.number.not-null")
    @Pattern(regexp = "^\\s*(?<digit>\\d{4})/(?<year>\\d{2})\\s*$", message = "validation.study.number.invalid-pattern")
    @Column(unique = true)
    private String number;

    @Column(nullable = false)
    private Instant creationDate;

    @Column(nullable = false)
    private Instant lastUpdate;

    @ManyToOne(optional = false)
    private ServiceUser lastUpdateUser;

    @ManyToMany
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JoinTable(
        name = "study_notion",
        joinColumns = @JoinColumn(name = "study_id"),
        inverseJoinColumns = @JoinColumn(name = "notion_id"))
    @Builder.Default
    private Set<Notion> notions = new HashSet<>();

    @NotNull(message = "validation.study.status.not-null")
    @Column(nullable = false)
    @Enumerated(STRING)
    @Builder.Default
    private StudyStatus status = StudyStatus.OPEN;

    @ManyToMany(fetch = LAZY)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JoinTable(
        name = "study_folder_attachment",
        joinColumns = @JoinColumn(name = "study_id"),
        inverseJoinColumns = @JoinColumn(name = "folder_attachment_id"))
    @Builder.Default
    private Set<FolderAttachment> attachments = new HashSet<>();

    @ElementCollection
    @CollectionTable(
        name = STUDY_REFERENCE_TABLE,
        joinColumns = @JoinColumn(name = "study_id")
    )
    @Schema(description = "List of folder references")
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private Set<StudyReference> references = new HashSet<>();

    @PrePersist
    public void prePersist() {
        id = randomUUID();
        Instant now = now();
        creationDate = now;
        lastUpdate = now;
        status = StudyStatus.OPEN;
    }

    public Study(StudyCreateDto dto, Folder folder, Set<Notion> notions, Set<FolderAttachment> attachments, ServiceUser serviceUser) {
        this.number = dto.getNumber();
        this.notions = notions;
        this.attachments = attachments;
        this.references = dto.getReferences().stream().map(StudyReference::new).collect(Collectors.toSet());
        this.lastUpdateUser = serviceUser;
        this.lastUpdate = now();
        this.folder = folder;
    }

    public StudyDto asDto(List<UUID> serviceIdsWithOperatorOrAdminRole) {
        return StudyDto.fromStudy(this, serviceIdsWithOperatorOrAdminRole);
    }
}
