package fr.shom.ping.utils;

import fr.shom.ping.model.Feature;
import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.dto.InfosourceToDisplayOnCreationMapDto;
import fr.shom.ping.model.tuple.InfosourceToDisplayOnCreationMapTuple;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

public final class InfosourceUtil {

    private InfosourceUtil() {
    }

    public static List<InfosourceToDisplayOnCreationMapDto> groupInfosourceToDisplayOnCreationMapTupleByInfosourceId(
        List<InfosourceToDisplayOnCreationMapTuple> tuples) {
        Map<UUID, List<Feature>> featuresByInfosourceId = tuples.stream()
            .collect(groupingBy(InfosourceToDisplayOnCreationMapTuple::getId, mapping(InfosourceToDisplayOnCreationMapTuple::getGeometry, toList())));

        return featuresByInfosourceId.keySet()
            .stream()
            .map(uuid -> InfosourceToDisplayOnCreationMapDto.builder()
                .id(uuid)
                .geometries(featuresByInfosourceId.get(uuid))
                .number(tuples.stream().filter(infosourceToDisplayOnCreationMapTuple -> infosourceToDisplayOnCreationMapTuple.getId() == uuid).findFirst().get().getNumber())
                .build())
            .collect(toList());
    }

    public static String generateIdentifierFromEmail(String email) {
        return StringUtils.substringBeforeLast(email, ".").replace("@", "_");
    }

    public static Instant getDeletionDate(Infosource infosource) {
        return infosource.getLastUpdate().plus(30, ChronoUnit.DAYS);
    }

    public static String buildNaturesOrderedArraySqlCase(String lang) {
        return "CASE infos.nature\n" +
            (Locale.FRENCH.getLanguage().equals(lang) ?
                "WHEN 'PUBLICATION_ANOMALY' THEN 1\n" +
                    "WHEN 'MAP_ANOMALY' THEN 2\n" +
                    "WHEN 'GEOREGULATION' THEN 3\n" +
                    "WHEN 'FUTURE_INFORMATION' THEN 4\n" +
                    "WHEN 'LIGHTHOUSE_AND_BEACON' THEN 5\n" +
                    "WHEN 'ACTIVITY_FORECAST' THEN 6\n" +
                    "WHEN 'OTHER_REPORT' THEN 7\n"
                :
                "WHEN 'ACTIVITY_FORECAST' THEN 1\n" +
                    "WHEN 'GEOREGULATION' THEN 2\n" +
                    "WHEN 'FUTURE_INFORMATION' THEN 3\n" +
                    "WHEN 'LIGHTHOUSE_AND_BEACON' THEN 4\n" +
                    "WHEN 'OTHER_REPORT' THEN 5\n" +
                    "WHEN 'HAZARD_REPORT' THEN 6\n" +
                    "WHEN 'MAP_ANOMALY' THEN 7\n"
            ) +
            "ELSE 8\n" +
            "END\n";
    }

    public static String buildStatusOrderedArraySqlCase(String lang) {
        return "CASE infos.status\n" +
            (Locale.FRENCH.getLanguage().equals(lang) ?
                "WHEN 'DRAFT' THEN 1\n" +
                    "WHEN 'SENT' THEN 2\n" +
                    "WHEN 'OPENED' THEN 3\n" +
                    "WHEN 'TAKEN' THEN 4\n" +
                    "WHEN 'REJECTED' THEN 5\n"
                :
                "WHEN 'DRAFT' THEN 1\n" +
                    "WHEN 'OPENED' THEN 2\n" +
                    "WHEN 'REJECTED' THEN 3\n" +
                    "WHEN 'SENT' THEN 4\n" +
                    "WHEN 'TAKEN' THEN 5\n"
            ) +
            "ELSE 6\n" +
            "END\n";
    }
}
