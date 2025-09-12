package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import static jakarta.persistence.FetchType.EAGER;
import static jakarta.persistence.FetchType.LAZY;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeoregulationNumber implements Serializable {
    @Serial
    private static final long serialVersionUID = -1527184925653181815L;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private String text;

    @ElementCollection(fetch = EAGER)
    @CollectionTable(
        name = "georegulation_number_authority_code"
    )
    @Column(name = "authority_code", nullable = false)
    @Builder.Default
    private List<String> authorityCodes = new ArrayList<>();

    @OneToMany(fetch = LAZY)
    @JoinTable(
        name = "georegulation_number_authority",
        joinColumns = @JoinColumn(name = "georegulation_id"),
        inverseJoinColumns = @JoinColumn(name = "authority_id")
    )
    @ToString.Exclude
    @JsonIgnore
    @Builder.Default
    private List<MaritimeService> authorities = new ArrayList<>();

    @Transient
    @Builder.Default
    private List<UUID> authorityIds = new ArrayList<>();

    private LocalDate date;

}
