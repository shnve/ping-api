package fr.shom.ping.model.dto;

import fr.shom.ping.model.FixedDateRange;
import fr.shom.ping.model.GeoregulationPart;
import fr.shom.ping.model.SeasonalPeriod;
import fr.shom.ping.model.enums.GeoregulationPartType;
import fr.shom.ping.model.enums.RestrictionVessel;
import fr.shom.ping.model.enums.UnitsGrossTonnage;
import fr.shom.ping.validation.NoHtml;
import fr.shom.ping.validation.ValidGeoregulationPartRestriction;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

import static fr.shom.ping.model.dto.GeoregulationCreateDto.updateCalendarPeriods;
import static java.util.stream.Collectors.toList;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidGeoregulationPartRestriction
public class GeoregulationPartCreateDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 2129469015586172064L;

    @NotEmpty(message = "validation.georegulation.features.not-empty")
    @Default
    private List<@Valid FeatureDto> features = new ArrayList<>();

    @Default
    private List<GeoregulationPartType> types = new ArrayList<>();

    @Default
    private List<@Valid FixedDateRange> calendarPeriods = new ArrayList<>();

    @Default
    private List<@Valid SeasonalPeriod> seasonalPeriods = new ArrayList<>();

    @NoHtml
    private String information;

    @NoHtml
    private String internationalInformation;

    private Integer lengthVessel;

    private RestrictionVessel choiceLengthVessel;

    private Integer grossTonnageVessel;

    private RestrictionVessel choiceGrossTonnageVessel;

    private UnitsGrossTonnage unitsGrossTonnageVessel;

    @NotNull(message = "validation.georegulation.is-commercial-ship.not-null")
    private Boolean isCommercialShip;

    public GeoregulationPart toGeoregulationPart(Integer position, boolean isTemporary) {
        if (isTemporary) {
            updateCalendarPeriods(calendarPeriods);
        } else {
            // When a saved temporary GR becomes permanent, we need to remove his potentials unuses calendar periods
            setCalendarPeriods(new ArrayList<>());
        }

        AtomicInteger seasonalPeriodPosition = new AtomicInteger(1);
        seasonalPeriods.forEach(seasonalPeriod -> seasonalPeriod.setPosition(seasonalPeriodPosition.getAndIncrement()));

        return GeoregulationPart.builder()
            .position(position)
            .features(features.stream().map(FeatureDto::toFeature).collect(toList()))
            .calendarPeriods(new TreeSet<>(calendarPeriods))
            .seasonalPeriods(new TreeSet<>(seasonalPeriods))
            .information(information)
            .internationalInformation(internationalInformation)
            .types(types)
            .lengthVessel(lengthVessel)
            .choiceLengthVessel(choiceLengthVessel)
            .grossTonnageVessel(grossTonnageVessel)
            .choiceGrossTonnageVessel(choiceGrossTonnageVessel)
            .unitsGrossTonnageVessel(unitsGrossTonnageVessel)
            .isCommercialShip(isCommercialShip)
            .build();
    }

    public static GeoregulationPartCreateDto fromGeoregulationPart(GeoregulationPart georegulationPart) {
        return GeoregulationPartCreateDto.builder()
            .features(georegulationPart.getFeatures().stream().map(FeatureDto::new).collect(Collectors.toList()))
            .calendarPeriods(new ArrayList<>(georegulationPart.getCalendarPeriods()))
            .seasonalPeriods(new ArrayList<>(georegulationPart.getSeasonalPeriods()))
            .information(georegulationPart.getInformation())
            .internationalInformation(georegulationPart.getInternationalInformation())
            .types(new ArrayList<>(georegulationPart.getTypes()))
            .lengthVessel(georegulationPart.getLengthVessel())
            .choiceLengthVessel(georegulationPart.getChoiceLengthVessel())
            .grossTonnageVessel(georegulationPart.getGrossTonnageVessel())
            .choiceGrossTonnageVessel(georegulationPart.getChoiceGrossTonnageVessel())
            .unitsGrossTonnageVessel(georegulationPart.getUnitsGrossTonnageVessel())
            .isCommercialShip(georegulationPart.getIsCommercialShip())
            .build();
    }
}
