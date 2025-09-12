package fr.shom.ping.model;

import fr.shom.ping.model.enums.NotionType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.InheritanceType.SINGLE_TABLE;
import static java.util.UUID.randomUUID;

@Data
@NoArgsConstructor
@SuperBuilder
@Entity
@Inheritance(strategy = SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
public abstract class Notion implements Serializable {
    @Serial
    private static final long serialVersionUID = -806131597155231950L;

    @Id
    private UUID id;

    @Enumerated(STRING)
    @Column(insertable = false, updatable = false)
    private NotionType type;

    @ManyToOne
    @JoinColumn(name = "folder_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Folder folder;

    @PrePersist
    public void prePersist() {
        id = randomUUID();
    }
}
