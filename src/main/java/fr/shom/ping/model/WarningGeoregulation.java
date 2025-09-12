package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.util.UUID.randomUUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = NON_NULL)
@Entity
@Table(name = "warning_georegulation")
public class WarningGeoregulation implements Serializable {
    @Serial
    private static final long serialVersionUID = 8160671412012684759L;

    @Id
    private UUID id;

    @OneToOne(optional = false)
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Warning warning;

    @OneToOne(optional = false)
    private Georegulation georegulation;

    @Builder.Default
    @Column(name = "is_public_reference", nullable = false)
    private Boolean isPublicReference = false;

    @PrePersist
    public void prePersist() {
        id = randomUUID();
    }
}
