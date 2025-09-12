package fr.shom.ping.model;

import fr.shom.ping.model.enums.GeoregulationPartType;
import fr.shom.ping.model.enums.RestrictionVessel;
import fr.shom.ping.model.enums.UnitsGrossTonnage;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static java.util.UUID.randomUUID;
import static jakarta.persistence.EnumType.STRING;

@Getter
@EqualsAndHashCode
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class GeoregulationPart implements Serializable, Comparable<GeoregulationPart> {
    @Serial
    private static final long serialVersionUID = -7424116529530413695L;

    @Id
    private UUID id;

    @Column(nullable = false)
    @Default
    private Integer position = 1;

    @ElementCollection
    @CollectionTable(
        name = "georegulation_part_feature",
        joinColumns = @JoinColumn(name = "part_id")
    )
    @Default
    private List<Feature> features = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
        name = "georegulation_part_type",
        joinColumns = @JoinColumn(name = "part_id")
    )
    @Enumerated(STRING)
    @Column(name = "type")
    @Default
    private List<GeoregulationPartType> types = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
        name = "fixed_date_range",
        joinColumns = @JoinColumn(name = "parent_id")
    )
    @Default
    private SortedSet<FixedDateRange> calendarPeriods = new TreeSet<>();

    @ElementCollection
    @CollectionTable(
        name = "georegulation_part_seasonal_period",
        joinColumns = @JoinColumn(name = "part_id")
    )
    @Default
    private SortedSet<SeasonalPeriod> seasonalPeriods = new TreeSet<>();

    private String information;

    private String internationalInformation;

    @Column
    private Integer lengthVessel;

    @Column
    @Enumerated(STRING)
    private RestrictionVessel choiceLengthVessel;

    @Column
    private Integer grossTonnageVessel;

    @Column
    @Enumerated(STRING)
    private RestrictionVessel choiceGrossTonnageVessel;

    @Column
    @Enumerated(STRING)
    private UnitsGrossTonnage unitsGrossTonnageVessel;

    @Column(nullable = false)
    @Default
    private Boolean isCommercialShip = false;

    @Override
    public int compareTo(GeoregulationPart part) {
        return position.compareTo(part.getPosition());
    }

    @PrePersist
    public void prePersist() {
        id = randomUUID();
    }
}
