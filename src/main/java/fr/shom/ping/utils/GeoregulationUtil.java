package fr.shom.ping.utils;

import fr.shom.ping.model.Feature;
import fr.shom.ping.model.Georegulation;
import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.dto.GeoregulationToDisplayOnCreationMapDto;
import fr.shom.ping.model.enums.GeoregulationType;
import fr.shom.ping.model.tuple.GeoregulationToDisplayOnCreationMapTuple;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;

import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

public final class GeoregulationUtil {

    private static final DateTimeFormatter NUMBER_DATE_PATTERN = ofPattern("ddMMuuuu");


    private GeoregulationUtil() {
    }

    public static List<GeoregulationToDisplayOnCreationMapDto> groupGeoregulationToDisplayOnCreationMapTupleByGeoregulationId(
        List<GeoregulationToDisplayOnCreationMapTuple> tuples, GeoregulationType georegulationType) {
        final Map<UUID, List<Feature>> featuresByGeoregulationId = tuples.stream()
            .collect(groupingBy(GeoregulationToDisplayOnCreationMapTuple::getId, mapping(GeoregulationToDisplayOnCreationMapTuple::getGeometry, toList())));

        return featuresByGeoregulationId.keySet()
            .stream()
            .map(uuid -> GeoregulationToDisplayOnCreationMapDto.builder()
                .id(uuid)
                .features(featuresByGeoregulationId.get(uuid))
                .type(georegulationType)
                .build())
            .collect(toList());
    }

    public static String getNumber(Georegulation georegulation) {
        return getNumber(
            georegulation.getNumber().getYear(),
            georegulation.getNumber().getText(),
            Set.copyOf(georegulation.getNumber().getAuthorityCodes()),
            georegulation.getNumber().getDate()
        );
    }

    public static String getNumber(Integer year, String text, Collection<String> authorityCodes, LocalDate numberDate) {
        return Stream.of(
                String.valueOf(year),
                text,
                authorityCodes.stream().sorted().collect(joining("-")),
                ofNullable(numberDate).map(date -> date.format(NUMBER_DATE_PATTERN)).orElse(null))
            .filter(StringUtils::isNotBlank)
            .collect(joining("-"));
    }

    public static String getFullNumber(Integer year, String text, List<MaritimeService> maritimeServices, LocalDate numberDate,
        boolean isInternationalLanguage) {
        return Stream.of(
                String.valueOf(year),
                text,
                maritimeServices.stream().map(service -> {
                        if (isInternationalLanguage) {
                            return ofNullable(service.getInternationalName()).orElse(service.getName());
                        }

                        return service.getName();
                    })
                    .collect(joining("-")),
                ofNullable(numberDate).map(date -> date.format(NUMBER_DATE_PATTERN)).orElse(null))
            .filter(StringUtils::isNotBlank)
            .collect(joining("-"));
    }

    public static Instant getTargetDateForNewStatus(Georegulation georegulation) {
        final LocalDateTime publicationDateTime = LocalDateTime.ofInstant(georegulation.getPublicationDate(), UTC);
        return publicationDateTime.plusMonths(georegulation.getMarkAsNewInMonths()).toInstant(UTC);
    }
}
