package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.shom.ping.model.enums.WarningReferenceCategoryEnum;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import static java.util.UUID.randomUUID;
import static jakarta.persistence.EnumType.STRING;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "warning_reference_as_public_reference")
public class WarningPublicReference implements Serializable {
    @Serial
    private static final long serialVersionUID = -6531426542982573594L;

    @Id
    private UUID id;

    @OneToOne(optional = false)
    @JoinColumn(name = "warning_id")
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    private Warning warning;

    @OneToOne(optional = false)
    @JoinColumn(name = "warning_reference_as_public_reference_id")
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    private Warning warningPublicReference;

    @Enumerated(STRING)
    @Column(nullable = false)
    private WarningReferenceCategoryEnum category;

    @PrePersist
    public void prePersist() {
        id = randomUUID();
    }
}
