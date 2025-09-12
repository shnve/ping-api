package fr.shom.ping.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@MappedSuperclass
public abstract class AbstractAttachment implements Serializable {
    @Serial
    private static final long serialVersionUID = 884696926307824286L;

    @Id
    private UUID id;

    @Column(nullable = false)
    private Instant creationDate;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private String identifier;

    @Column(nullable = false)
    private Long totalSize;

    @Column(nullable = false)
    private String type;

    @PrePersist
    public void prePersist() {
        id = randomUUID();
        creationDate = now();
    }
}
