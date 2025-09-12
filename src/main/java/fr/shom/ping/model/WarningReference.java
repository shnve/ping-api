package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.shom.ping.model.enums.WarningReferenceCategoryEnum;
import java.io.Serial;
import java.io.Serializable;
import java.util.Set;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static fr.shom.ping.utils.SchemaUtil.WARNING_REFERENCE_TABLE;
import static fr.shom.ping.utils.SchemaUtil.WARNING_REFERENCE_WARNING_TABLE;
import static java.util.UUID.randomUUID;
import static jakarta.persistence.EnumType.STRING;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = WARNING_REFERENCE_TABLE)
public class WarningReference implements Serializable {
    @Serial
    private static final long serialVersionUID = 4705421401716248826L;

    @Id
    private UUID id;

    @OneToOne(optional = false)
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    private Warning warning;

    @OneToMany
    @JoinTable(
        name = WARNING_REFERENCE_WARNING_TABLE,
        joinColumns = @JoinColumn(name = "warning_reference_id"),
        inverseJoinColumns = @JoinColumn(name = "warning_id"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Warning> warnings;

    @Column(nullable = false)
    private Boolean noMessageOnHand;

    @Enumerated(STRING)
    @Column(nullable = false)
    private WarningReferenceCategoryEnum category;

    @PrePersist
    public void prePersist() {
        id = randomUUID();
    }
}
