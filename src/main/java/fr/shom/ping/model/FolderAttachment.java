package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;

@EqualsAndHashCode
@ToString
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class FolderAttachment {
    private static final long serialVersionUID = -7399820644542173822L;

    @Id
    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @Schema(description = "Creation date time (UTC)")
    @Column(nullable = false)
    private Instant creationDate;

    @Schema(description = "Internal comment")
    private String comment;

    @Schema(description = "Reception date time (UTC)")
    @Column(nullable = false)
    private Instant receptionDate;

    @JsonIgnore
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne(optional = false)
    private Folder folder;

    @JsonIgnore
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne
    private MaritimeService maritimeService;

    @JsonIgnore
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @OneToOne(optional = false, cascade = ALL)
    private Attachment attachment;

    @ManyToMany(fetch = LAZY)
    @Schema(description = "List of folder studies linked")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JoinTable(
        name = "study_folder_attachment",
        joinColumns = @JoinColumn(name = "folder_attachment_id"),
        inverseJoinColumns = @JoinColumn(name = "study_id"))
    @Builder.Default
    private Set<Study> studies = new HashSet<>();

    @PrePersist
    public void prePersist() {
        id = randomUUID();
        creationDate = now();
        if (receptionDate == null) {
            receptionDate = creationDate;
        }
    }
}
